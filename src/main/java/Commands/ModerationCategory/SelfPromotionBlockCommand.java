package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import Core.*;
import Core.Mention.MentionTools;
import Core.Utils.StringUtil;
import MySQL.Modules.SPBlock.DBSPBlock;
import MySQL.Modules.SPBlock.SPBlockBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

@CommandProperties(
    trigger = "spblock",
    botPermissions = Permission.MANAGE_MESSAGES | Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    userPermissions = Permission.MANAGE_MESSAGES | Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    emoji = "\uD83D\uDEE1️",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/ok-shield-icon.png",
    executable = true
)
public class SelfPromotionBlockCommand extends Command implements OnNavigationListener {
    
    private SPBlockBean spBlockBean;
    private CustomObservableList<User> ignoredUsers, logReceivers;
    private CustomObservableList<ServerTextChannel> ignoredChannels;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        spBlockBean = DBSPBlock.getInstance().getBean(event.getServer().get().getId());
        ignoredUsers = spBlockBean.getIgnoredUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        logReceivers = spBlockBean.getLogReceiverUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        ignoredChannels = spBlockBean.getIgnoredChannelIds().transform(userId -> event.getServer().get().getTextChannelById(userId), DiscordEntity::getId);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionTools.getUsers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlockBean.getIgnoredUserIds().clear();
                    spBlockBean.getIgnoredUserIds().addAll(userIgnoredList.stream().map(DiscordEntity::getId).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                ArrayList<ServerTextChannel> channelIgnoredList = MentionTools.getTextChannels(event.getMessage(), inputString).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlockBean.getIgnoredChannelIds().clear();
                    spBlockBean.getIgnoredChannelIds().addAll(channelIgnoredList.stream().map(DiscordEntity::getId).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                ArrayList<User> logRecieverList = MentionTools.getUsers(event.getMessage(), inputString).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlockBean.getLogReceiverUserIds().clear();
                    spBlockBean.getLogReceiverUserIds().addAll(logRecieverList.stream().map(DiscordEntity::getId).collect(Collectors.toList()));

                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return Response.TRUE;
                }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        spBlockBean.toggleActive();
                        setLog(LogStatus.SUCCESS, getString("onoffset", !spBlockBean.isActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        setState(3);
                        return true;

                    case 4:
                        setState(4);
                        return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        spBlockBean.getIgnoredUserIds().clear();
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                        return true;
                }
                return false;

            case 2:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        spBlockBean.getIgnoredChannelIds().clear();
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                        return true;
                }
                return false;

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        spBlockBean.getLogReceiverUserIds().clear();
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                        return true;
                }
                return false;

            case 4:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i <= 2) {
                    spBlockBean.setAction(SPBlockBean.ActionList.values()[i]);
                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("actionset"));
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), spBlockBean.isActive()), true)
                       .addField(getString("state0_mignoredusers"), new ListGen<User>().getList(ignoredUsers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mignoredchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), true)
                       .addField(getString("state0_mlogreciever"), new ListGen<User>().getList(logReceivers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_maction"),getString("state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true);

            case 1:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(getString("state0_mactionlist").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {
    }

    @Override
    public int getMaxReactionNumber() {
        return 5;
    }

}
