package commands;

import commands.listeners.*;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import constants.*;
import core.*;
import core.emojiconnection.EmojiConnection;
import core.schedule.MainScheduler;
import core.utils.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Command {

    private final static Logger LOGGER = LoggerFactory.getLogger(Command.class);
    protected static final int DEFAULT_STATE = 0;

    private final long id = System.nanoTime();
    private final String category;
    private final String prefix;
    private final CommandProperties commandProperties;
    private Message starterMessage, navigationMessage, lastUserMessage;
    private Locale locale;
    private LoadingStatus loadingStatus = LoadingStatus.OFF;
    private long reactionMessageID = -1, reactionUserID = -1, forwardChannelID = -1, forwardUserID = -1;
    private Countdown countdown;
    private LogStatus logStatus = null;
    private String log;
    private String[] options;
    private boolean navigationActive, loadingBlock = false, navigationPrivateMessage = false;
    private int state = DEFAULT_STATE, page = 0, pageMax = 0;
    private final HashMap<Object, Object> attachments = new HashMap<>();
    private final ArrayList<Runnable> completedListeners = new ArrayList<>();

    private enum LoadingStatus { OFF, ONGOING, FINISHED }

    public Command(Locale locale, String prefix) {
        this.locale = locale;
        this.prefix = prefix;

        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = CategoryCalculator.getCategoryByCommand(this.getClass());
    }

    protected abstract boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable;

    public void onReceivedSuper(MessageCreateEvent event, String followedString) {
        if (this instanceof PingCommand) getAttachments().put(13, Instant.now());

        starterMessage = event.getMessage();
        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        try {
            onMessageReceived(event, followedString);
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, this, event.getServerTextChannel().get());
            return;
        } finally {
            removeLoadingReaction();
        }

        if ((this instanceof OnReactionAddListener)) {
            Message reactionMessage = ((OnReactionAddListener) this).getReactionMessage();
            if (reactionMessage != null) {
                reactionUserID = event.getMessage().getAuthor().getId();
                addReactionListener(reactionMessage);
            }
        }

        if ((this instanceof OnForwardedRecievedListener)) {
            Message forwardedMessage = ((OnForwardedRecievedListener) this).getForwardedMessage();
            if (forwardedMessage != null) {
                reactionUserID = event.getMessage().getAuthor().getId();
                addForwarder(forwardedMessage, event.getMessage().getServerTextChannel().get(), starterMessage.getUserAuthor().get());
            }
        }

        if (this instanceof OnNavigationListener) {
            addNavigation(navigationMessage, event.getChannel(), event.getMessage().getUserAuthor().get());
        }
    }

    public void onReactionAddSuper(SingleReactionEvent event) {
        if (countdown != null) countdown.reset();

        try {
            ((OnReactionAddListener) this).onReactionAdd(event);
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getServerTextChannel().get());
        }
    }

    public boolean onForwardedRecievedSuper(MessageCreateEvent event) {
        Response success = null;
        if (commandProperties.withLoadingBar()) addLoadingReaction(event.getMessage());
        if (countdown != null) countdown.reset();

        try {
            success = ((OnForwardedRecievedListener) this).onForwardedRecieved(event);
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getServerTextChannel().get());
        } finally {
            removeLoadingReaction(event.getMessage());
        }

        return success != null;
    }

    public boolean onNavigationMessageSuper(MessageCreateEvent event, String followedString, boolean firstTime) {
         resetNavigation();

        if (firstTime) {
            starterMessage = event.getMessage();
            reactionUserID = starterMessage.getUserAuthor().get().getId();
            ServerTextChannel channel = event.getServerTextChannel().get();

            if (this instanceof HelpCommand && (!channel.canYouWrite() || !channel.canYouAddNewReactions() || !channel.canYouEmbedLinks()))
                navigationPrivateMessage = true;
        }

        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        Response success = runMessageCreate(event, followedString, firstTime);
        if (success == Response.ERROR) return true;

        addNavigationEmojis(event, firstTime, success);
        return success != null;
    }

    private Response runMessageCreate(MessageCreateEvent event, String followedString, boolean firstTime) {
        Response success;
        try {
            navigationActive = true;
            if (firstTime) success = onMessageReceived(event, followedString) ? Response.TRUE : Response.FALSE;
            else success = controllerMessage(event, followedString, state);

            if ((success != null || navigationMessage == null) && (!firstTime || success == Response.TRUE) && navigationActive) {
                if (countdown != null) countdown.reset();
                drawSuper(event.getApi(), event.getServerTextChannel().get());
            }
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getServerTextChannel().get());
            return Response.ERROR;
        } finally {
            removeLoadingReaction();
        }

        return success;
    }

    private void addNavigationEmojis(MessageCreateEvent event, boolean firstTime, Response success) {
        if (firstTime) {
            if (success == Response.TRUE && navigationActive) {
                for (int i = -1; i < ((OnNavigationListener) this).getMaxReactionNumber(); i++) {
                    if (i == -1) {
                        if (navigationMessage != null) {
                            if (navigationMessage.getChannel().canYouUseExternalEmojis())
                                navigationMessage.addReaction(DiscordUtil.createCustomEmojiFromTag(Emojis.BACK_EMOJI));
                            else navigationMessage.addReaction(Emojis.BACK_EMOJI_UNICODE);
                        }
                    }
                    if (i >= 0 && navigationMessage != null) {
                        navigationMessage.addReaction(LetterEmojis.LETTERS[i]);
                    }
                }
                if (navigationMessage != null) addNavigation(navigationMessage,navigationMessage.getChannel(), event.getMessage().getUserAuthor().get());
            }
        } else {
            if (success == Response.TRUE)
                event.getMessage().delete();
        }
    }

    public void onNavigationReactionSuper(SingleReactionEvent event) {
        if (countdown != null) countdown.reset();

        int index = getIndex(event);
        boolean changed = true;
        try {
            AtomicBoolean startCalculation = new AtomicBoolean(false);
            index = reactionPageChangeAndGetNewIndex(index, startCalculation);

            if (startCalculation.get())
                changed = controllerReaction(event, index, state);

            if (changed)
                drawSuper(event.getApi(), event.getChannel());
        } catch (Throwable throwable) {
            ExceptionUtil.handleCommandException(throwable, this, event.getChannel());
        }
    }

    private int reactionPageChangeAndGetNewIndex(int index, AtomicBoolean startCalculation) {
        int max = ((OnNavigationListener) this).getMaxReactionNumber();
        if (index >= max - 2 && options != null && options.length > max) {
            if (index == max - 2) {
                page--;
                if (page < 0) page = pageMax;
            } else if (index == max - 1) {
                page++;
                if (page > pageMax) page = 0;
            }
            resetNavigation();
            startCalculation.set(false);
            return index;
        } else {
            if (options != null && options.length > max && index >= 0)
                index += (max - 2) * page;
            resetNavigation();
            startCalculation.set(true);
            return index;
        }
    }

    private int getIndex(SingleReactionEvent event) {
        if ((event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Emojis.BACK_EMOJI_UNICODE)) ||
                (event.getEmoji().isCustomEmoji() && DiscordUtil.emojiIsString(event.getEmoji().asCustomEmoji().get(), Emojis.BACK_EMOJI))
        ) {
            return -1;
        } else {
            for(int i = 0; i < ((OnNavigationListener) this).getMaxReactionNumber(); i++) {
                if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(LetterEmojis.LETTERS[i]))
                    return i;
            }
        }

        return -2;
    }

    public void drawSuper(DiscordApi api, TextChannel channel) throws Throwable {
        EmbedBuilder eb = draw(api, state);

        int max = ((OnNavigationListener) this).getMaxReactionNumber();

        if (options != null && options.length > 0) {
            String[] newOptions;

            if (options.length <= max) { newOptions = options; }
            else {
                newOptions = new String[max];
                Arrays.fill(newOptions, "");
                if (Math.min(max - 2, options.length - (max - 2) * page) >= 0)
                    System.arraycopy(options, page * (max - 2), newOptions, 0, Math.min(max - 2, options.length - (max - 2) * page));

                newOptions[max - 2] = TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous");
                newOptions[max - 1] = TextManager.getString(getLocale(), TextManager.GENERAL, "list_next");
            }

            String str = EmojiConnection.getOptionsString(channel, false, options.length > max ? max - 2 : -1, newOptions);
            eb.addField(Emojis.EMPTY_EMOJI, Emojis.EMPTY_EMOJI);
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), str);
        }

        EmbedUtil.addLog(eb, logStatus, log);
        if (options != null && options.length > max)
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));

        try {
            if (channel.getCurrentCachedInstance().isPresent()) {
                if (navigationMessage == null) {
                    if (navigationPrivateMessage) {
                        if (channel.canYouAddNewReactions()) starterMessage.addReaction("✉").get();
                        navigationMessage = starterMessage.getUserAuthor().get().sendMessage(eb).get();
                    } else if (channel.canYouWrite() && channel.canYouSee() && channel.canYouEmbedLinks())
                        navigationMessage = channel.sendMessage(eb).get();
                } else {
                    navigationMessage.edit(eb).get();
                }
            }
        } catch (ExecutionException e) {
            if (!ExceptionUtil.exceptionIsClass(e, org.javacord.api.exception.UnknownMessageException.class))
                throw e;
        }
    }

    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        for(Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == state) {
                try {
                    return ((EmbedBuilder) method.invoke(this, api));
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for(Method method : getClass().getDeclaredMethods()) {
            Draw c = method.getAnnotation(Draw.class);
            if (c != null && c.state() == -1) {
                try {
                    return ((EmbedBuilder) method.invoke(this, api));
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        throw new Exception("State not found");
    }

    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        for(Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == state) {
                try {
                    return (Response) method.invoke(this, event, inputString);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for(Method method : getClass().getDeclaredMethods()) {
            ControllerMessage c = method.getAnnotation(ControllerMessage.class);
            if (c != null && c.state() == -1) {
                try {
                    return (Response) method.invoke(this, event, inputString);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        return null;
    }

    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        for(Method method : getClass().getDeclaredMethods()) {
            ControllerReaction c = method.getAnnotation(ControllerReaction.class);
            if (c != null && c.state() == state) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        for(Method method : getClass().getDeclaredMethods()) {
            ControllerReaction c = method.getAnnotation(ControllerReaction.class);
            if (c != null && c.state() == -1) {
                try {
                    return (boolean) method.invoke(this, event, i);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }

        return false;
    }

    private void resetNavigation() {
        log = "";
        logStatus = null;
    }

    public void addLoadingReaction() {
        addLoadingReaction(lastUserMessage);
    }

    private void addLoadingReaction(Message message) {
        if (
                loadingStatus != LoadingStatus.FINISHED &&
                        message != null &&
                        message.getCurrentCachedInstance().isPresent() &&
                        message.getChannel().canYouAddNewReactions() &&
                        !loadingBlock &&
                        message.getReactions().stream().map(Reaction::getEmoji)
                                .noneMatch(emoji -> DiscordUtil.emojiIsString(emoji, Emojis.LOADING) ||
                                        emoji.equalsEmoji("⏳"))
        ) {
            loadingStatus = LoadingStatus.ONGOING;

            CompletableFuture<Void> loadingBarReaction;
            if (message.getChannel().canYouUseExternalEmojis())
                loadingBarReaction = message.addReaction(DiscordUtil.createCustomEmojiFromTag(Emojis.LOADING));
            else loadingBarReaction = message.addReaction("⏳");
            loadingBarReaction.thenRun(() -> loadingStatus = LoadingStatus.FINISHED);
        }
    }

    public void removeLoadingReaction() {
        removeLoadingReaction(lastUserMessage);
    }

    private void removeLoadingReaction(Message message) {
        if (loadingStatus != LoadingStatus.OFF) {
            MainScheduler.getInstance().poll(100, "command_remove_loading_reaction", () -> {
                if (loadingStatus == LoadingStatus.ONGOING)
                    return true;

                loadingStatus = LoadingStatus.OFF;
                if (message.getCurrentCachedInstance().isPresent()) {
                    if (message.getChannel().canYouUseExternalEmojis())
                        message.removeOwnReactionByEmoji(DiscordUtil.createCustomEmojiFromTag(Emojis.LOADING));
                    else message.removeOwnReactionByEmoji("⏳");
                }
                return false;
            });
        }
    }

    public void addReactionListener(Message message) {
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void addForwarder(Message message, TextChannel forwardChannel, User forwardUser) {
        forwardChannelID = forwardChannel.getId();
        forwardUserID = forwardUser.getId();
        CommandContainer.getInstance().addMessageForwardListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void addNavigation(Message message, TextChannel forwardChannel, User forwardUser) {
        forwardChannelID = forwardChannel.getId();
        forwardUserID = forwardUser.getId();
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        CommandContainer.getInstance().addMessageForwardListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void onTimeOut(Message message) {
        if (commandProperties.turnOffTimeout()) return;
        if (CommandContainer.getInstance().reactionListenerContains(this)) {
            if (this instanceof OnNavigationListener) {
                try {
                    ((OnNavigationListener) this).onNavigationTimeOut(message);
                    if (commandProperties.deleteOnTimeOut()) removeNavigationWithMessage();
                    else removeNavigation();
                } catch (Throwable throwable) {
                    ExceptionUtil.handleCommandException(throwable, this, message.getServerTextChannel().get());
                }
            } else if (this instanceof OnReactionAddListener) {
                try {
                    if (commandProperties.deleteOnTimeOut()) removeReactionListenerWithMessage();
                    else removeReactionListener();
                    ((OnReactionAddListener) this).onReactionTimeOut(message);
                } catch (Throwable throwable) {
                    ExceptionUtil.handleCommandException(throwable, this, message.getServerTextChannel().get());
                }
            }
        } else if (CommandContainer.getInstance().forwarderContains(this)) {
            removeMessageForwarder();
            if (this instanceof OnForwardedRecievedListener) {
                try {
                    ((OnForwardedRecievedListener) this).onForwardedTimeOut();
                } catch (Throwable throwable) {
                    ExceptionUtil.handleCommandException(throwable, this, message.getServerTextChannel().get());
                }
            }
        }
    }

    public void removeNavigationWithMessage() {
        removeNavigation();
        try {
            if (starterMessage.getChannel().canYouManageMessages() && navigationMessage.getChannel() == starterMessage.getChannel())
                starterMessage.getChannel().bulkDelete(navigationMessage, starterMessage).get();
            else navigationMessage.delete().get();
        } catch (ExecutionException e) {
            //Ignore
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }
    }

    public void removeReactionListenerWithMessage() {
        Message reactionMessage = ((OnReactionAddListener) this).getReactionMessage();
        removeReactionListener(reactionMessage);

         if (starterMessage.getChannel().canYouManageMessages()) {
            starterMessage.getChannel().bulkDelete(reactionMessage, starterMessage).exceptionally(e -> {
                if (reactionMessage != null)
                    reactionMessage.delete(); //No log
                return null;
            });
        } else {
            if (reactionMessage != null)
                reactionMessage.delete(); //No log
        }
    }

    public void removeMessageForwarder() {
        CommandContainer.getInstance().removeForwarder(this);
    }

    public void removeReactionListener() {
        Message message = ((OnReactionAddListener) this).getReactionMessage();
        removeReactionListener(message);
    }

    public void removeReactionListener(Message message) {
        CommandContainer.getInstance().removeReactionListener(this);
        if (message != null && message.getChannel().canYouRemoveReactionsOfOthers())
            message.removeAllReactions();
    }

    public void removeNavigation() {
        navigationActive = false;
        removeMessageForwarder();
        removeReactionListener(navigationMessage);
    }

    public void stopCountdown() {
        if (countdown != null) countdown.stop();
    }

    public long getId() {
        return id;
    }

    public String getString(String key, String... args) {
        String text = TextManager.getString(locale, category,commandProperties.trigger()+"_"+key, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, int option, String... args) {
        String text = TextManager.getString(locale, category,commandProperties.trigger()+"_"+key, option, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, boolean secondOption, String... args) {
        String text = TextManager.getString(locale, category,commandProperties.trigger()+"_"+key, secondOption, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public CommandLanguage getCommandLanguage() {
        String title = getString("title");
        String descLong = getString("helptext");
        String usage = getString("usage");
        String examples = getString("examples");
        return new CommandLanguage(title, descLong, usage, examples);
    }

    public boolean checkWriteInChannelWithLog(ServerTextChannel channel) {
        if (channel.getCurrentCachedInstance().isPresent() && channel.canYouSee() && channel.canYouWrite() && channel.canYouEmbedLinks()) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#" + channel.getName()));
        return false;
    }

    public boolean checkManageChannelWithLog(ServerChannel channel) {
        if (PermissionUtil.botHasChannelPermission(channel, PermissionType.MANAGE_CHANNELS)) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_permission", (channel.asTextChannel().isPresent() ? "#" : "") + channel.getName()));
        return false;
    }

    public boolean checkRoleWithLog(Role role) {
        if (PermissionUtil.canYouManageRole(role)) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, "@" + role.getName()));
        return false;
    }

    public boolean checkRolesWithLog(List<Role> roles, User requester) {
        ArrayList<Role> unmanagableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!PermissionUtil.canYouManageRole(role)) unmanagableRoles.add(role);
        }

        if (unmanagableRoles.size() == 0) {
            ArrayList<Role> forbiddenRoles = new ArrayList<>();

            if (requester == null) requester = DiscordApiManager.getInstance().getYourself();
            for(Role role: roles) {
                if (!PermissionUtil.canManageRole(requester, role)) forbiddenRoles.add(role);
            }

            if (forbiddenRoles.size() == 0) return true;
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getMentionText().replace("**", "")));
            return false;
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanagableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanagableRoles).getMentionText().replace("**", "")));
        return false;
    }

    //Just getters and setters, nothing important
    public Server getServer() { return starterMessage.getServer().get(); }
    public String getPrefix() { return prefix; }
    public String getCategory() { return category; }
    public void setLocale(Locale locale) { this.locale = locale; }
    public Locale getLocale() {
        if (locale == null) LOGGER.error("Locale is null");
        return locale;
    }
    public long getReactionMessageID() { return reactionMessageID; }
    public long getForwardChannelID() { return forwardChannelID; }
    public long getForwardUserID() { return forwardUserID; }
    public Message getStarterMessage() { return starterMessage; }
    public boolean isNavigationPrivateMessage() { return navigationPrivateMessage; }
    public void setReactionUserID(long reactionUserID) { this.reactionUserID = reactionUserID; }
    public long getReactionUserID() { return reactionUserID; }
    public void setState(int state) {
        this.options = null;
        this.page = 0;
        this.state = state;
    }
    public Message getNavigationMessage() { return navigationMessage; }

    public String getTrigger() { return commandProperties.trigger(); }
    public String[] getAliases() { return commandProperties.aliases(); }
    public String getEmoji() { return commandProperties.emoji(); }
    public boolean isNsfw() { return commandProperties.nsfw(); }
    public boolean isExecutableWithoutArgs() { return commandProperties.executableWithoutArgs(); }
    public boolean requiresEmbeds() { return commandProperties.requiresEmbeds(); }
    public int getMaxCalculationTimeSec() { return commandProperties.maxCalculationTimeSec(); }
    public boolean isPatreonRequired() { return commandProperties.patreonRequired(); }

    public int getUserPermissions() {
        int perm = commandProperties.userPermissions();
        if ((perm & Permission.ADMINISTRATOR) != 0)
            return Permission.ADMINISTRATOR;

        if (this instanceof OnReactionAddListener || this instanceof OnNavigationListener || this instanceof OnReactionAddStaticListener) {
            perm |= Permission.READ_MESSAGE_HISTORY;
        }
        return perm;
    }

    public boolean isModCommand() {
        return (commandProperties.userPermissions() & ~Permission.READ_MESSAGE_HISTORY) != 0;
    }

    public int getBotPermissions() {
        int perm = commandProperties.botPermissions();
        if ((perm & Permission.ADMINISTRATOR) != 0)
            return Permission.ADMINISTRATOR;

        if (this instanceof OnReactionAddListener || this instanceof OnNavigationListener || this instanceof OnReactionAddStaticListener) {
            perm |= Permission.ADD_REACTIONS | Permission.READ_MESSAGE_HISTORY;
        }
        return perm;
    }

    public boolean canRunOnServer(long serverId, long userId) {
        long[] allowedServerIds = commandProperties.exlusiveServers();
        long[] allowedUserIds = commandProperties.exlusiveUsers();

        return ((allowedServerIds.length == 0) || Arrays.stream(allowedServerIds).anyMatch(checkServerId -> checkServerId == serverId)) &&
                ((allowedUserIds.length == 0) || Arrays.stream(allowedUserIds).anyMatch(checkUserId -> checkUserId == userId)) &&
                (!commandProperties.onlyPublicVersion() || Bot.isPublicVersion());
    }

    public Optional<LocalDate> getReleaseDate() {
        int[] releaseDateArray = commandProperties.releaseDate();
        return Optional.ofNullable(releaseDateArray.length == 3 ? LocalDate.of(releaseDateArray[0], releaseDateArray[1], releaseDateArray[2]) : null);
    }

    public boolean hasTimeOut() { return !commandProperties.turnOffTimeout(); }
    public void blockLoading() { loadingBlock = true; }
    public int getState() { return state; }

    public String[] getOptions() { return options; }
    public void setOptions(String[] options) {
        this.options = options;
        if (options != null) {
            int maxReactions = ((OnNavigationListener) this).getMaxReactionNumber();
            if (maxReactions > 2)
                this.pageMax = Math.max(0, options.length - 1) / (maxReactions - 2);
        }
    }
    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }
    public int getPage() {
        return page;
    }

    public HashMap<Object, Object> getAttachments() {
        return attachments;
    }

    public void addCompletedListener(Runnable runnable) {
        completedListeners.add(runnable);
    }

    public ArrayList<Runnable> getCompletedListeners() {
        return completedListeners;
    }

    public static CommandProperties getClassProperties(Class<? extends Command> c) {
        return c.getAnnotation(CommandProperties.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerMessage {
        int state() default -1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ControllerReaction {
        int state() default -1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Draw {
        int state() default -1;
    }

}