package events.discordevents.servermemberleave;

import commands.runnables.utilitycategory.WelcomeCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberLeaveAbstract;
import modules.Welcome;
import mysql.modules.server.DBServer;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@DiscordEvent(allowBots = true)
public class ServerMemberLeaveWelcome extends ServerMemberLeaveAbstract {

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
        if (welcomeMessageBean.isGoodbyeActive()) {
            welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                    User user = event.getUser();
                    channel.sendMessage(
                            StringUtil.defuseMassPing(
                                    Welcome.resolveVariables(
                                            welcomeMessageBean.getGoodbyeText(),
                                            StringUtil.escapeMarkdown(server.getName()),
                                            user.getMentionTag(),
                                            StringUtil.escapeMarkdown(user.getName()),
                                            StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                                            StringUtil.numToString(server.getMemberCount())
                                    )
                            )
                    ).exceptionally(ExceptionLogger.get());
                }
            });
        }

        return true;
    }

}
