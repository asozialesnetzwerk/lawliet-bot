package events.discordevents.messagecreate;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnForwardedRecievedListener;
import commands.listeners.OnNavigationListener;
import commands.runnables.gimmickscategory.QuoteCommand;
import commands.runnables.informationcategory.HelpCommand;
import core.utils.ExceptionUtil;
import core.DiscordApiManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import mysql.modules.autoquote.DBAutoQuote;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEvent(priority = EventPriority.LOW)
public class MessageCreateCommand extends MessageCreateAbstract {

    final Logger LOGGER = LoggerFactory.getLogger(MessageCreateCommand.class);

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        String prefix = serverBean.getPrefix();
        String content = event.getMessage().getContent();

        if (content.toLowerCase().startsWith("i.") && prefix.equalsIgnoreCase("L."))
            content = prefix + content.substring(2);

        String[] prefixes = {
                prefix,
                DiscordApiManager.getInstance().getYourself().getMentionTag(),
                "<@!" + DiscordApiManager.getInstance().getYourself().getIdAsString() + ">"
        };

        int prefixFound = -1;
        for (int i = 0; i < prefixes.length; i++) {
            if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                prefixFound = i;
                break;
            }
        }

        if (prefixFound > -1) {
            if (prefixFound > 0 && manageForwardedMessages(event)) return true;

            String newContent = StringUtil.trimString(content.substring(prefixes[prefixFound].length()));
            if (newContent.contains("  ")) newContent = newContent.replace("  ", " ");
            String commandTrigger = newContent.split(" ")[0].toLowerCase();
            if (newContent.contains("<") && newContent.split("<")[0].length() < commandTrigger.length())
                commandTrigger = newContent.split("<")[0].toLowerCase();

            String followedString;
            try {
                followedString = StringUtil.trimString(newContent.substring(commandTrigger.length()));
            } catch (StringIndexOutOfBoundsException e) {
                followedString = "";
            }

            if (commandTrigger.length() > 0) {
                Locale locale = serverBean.getLocale();
                Class<? extends Command> clazz;
                clazz = CommandContainer.getInstance().getCommandMap().get(commandTrigger);
                if (clazz != null) {
                    Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                    if (!command.isExecutableWithoutArgs() && followedString.isEmpty()) {
                        followedString = command.getTrigger();
                        command = CommandManager.createCommandByClass(HelpCommand.class, locale, prefix);
                        command.getAttachments().put("noargs", true);
                    }

                    try {
                        CommandManager.manage(event, command, followedString, getStartTime());
                    } catch (Throwable e) {
                        ExceptionUtil.handleCommandException(e, command, event.getServerTextChannel().get());
                    }
                }
            }
        } else {
            if (manageForwardedMessages(event)) return true;
            checkAutoQuote(event);
        }

        return true;
    }

    private void checkAutoQuote(MessageCreateEvent event) throws ExecutionException {
        if (event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            Locale locale = serverBean.getLocale();
            ArrayList<Message> messages = MentionUtil.getMessagesFromLinks(event.getMessage(), event.getMessage().getContent()).getList();
            if (messages.size() > 0 && DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).isActive()) {
                try {
                    for (int i = 0; i < Math.min(3, messages.size()); i++) {
                        Message message = messages.get(i);
                        QuoteCommand quoteCommand = new QuoteCommand(locale, serverBean.getPrefix());
                        quoteCommand.postEmbed(event.getServerTextChannel().get(), message, true);
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Exception in Auto Quote", throwable);
                }
            }
        }
    }

    private boolean manageForwardedMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if (command != null &&
                    (event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) &&
                    (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)
            ) {
                try {
                    if (command instanceof OnForwardedRecievedListener) {
                        boolean end = command.onForwardedRecievedSuper(event);
                        if (end) return true;
                    }
                    if (command instanceof OnNavigationListener) {
                        boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                        if (end) return true;
                    }
                } catch (Throwable e) {
                    ExceptionUtil.handleCommandException(e, command, event.getServerTextChannel().get());
                }
            }
        }

        return false;
    }

}
