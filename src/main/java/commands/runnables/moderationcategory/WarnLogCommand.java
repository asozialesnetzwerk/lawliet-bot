package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningsBean;
import mysql.modules.warning.ServerWarningsSlot;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "warnlog",
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true,
        aliases = {"warns"}
)
public class WarnLogCommand extends UserAccountAbstract {

    public WarnLogCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().getBean(new Pair<>(server.getId(), user.getId()));

        StringBuilder latestWarnings = new StringBuilder();

        List<ServerWarningsSlot> slots = serverWarningsBean.getLatest(3);
        Collections.reverse(slots);
        for(ServerWarningsSlot serverWarningsSlot: slots) {
            Optional<User> requestor = serverWarningsSlot.getRequesterUser();
            Optional<String> reason = serverWarningsSlot.getReason();
            String userString = requestor.isPresent() ? (server.getMembers().contains(requestor.get()) ? requestor.get().getMentionTag() : String.format("**%s**", StringUtil.escapeMarkdown(requestor.get().getName()))) : TextManager.getString(getLocale(), TextManager.GENERAL, "unknown_user");
            String timeDiffString = TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), serverWarningsSlot.getTime(), true);
            latestWarnings.append(getString("latest_slot", reason.isPresent(), userString, timeDiffString, reason.orElse(getString("noreason"))));
        }

        String latestWarningsString = latestWarnings.toString();
        if (latestWarningsString.isEmpty()) latestWarningsString = TextManager.getString(getLocale(), TextManager.GENERAL, "empty");

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle("")
                .setAuthor(getString("author", getEmoji(), StringUtil.escapeMarkdown(user.getDisplayName(server))))
                .setThumbnail(user.getAvatar().getUrl().toString());
        eb.addField(getString("latest"), latestWarningsString, false);
        eb.addField(getString("amount"), getString("amount_template",
                StringUtil.numToString(serverWarningsBean.getAmountLatest(24, ChronoUnit.HOURS).size()),
                StringUtil.numToString(serverWarningsBean.getAmountLatest(7, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsBean.getAmountLatest(30, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsBean.getWarnings().size())
        ), false);

        return eb;
    }

}
