package events.discordevents.messagecreate;

import constants.AssetIds;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.BumpReminder;
import mysql.modules.bump.DBBump;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@DiscordEvent(priority = EventPriority.LOW, allowBots = true)
public class MessageCreateAnicordBump extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == AssetIds.ANICORD_SERVER_ID && event.getMessageAuthor().getId() == 302050872383242240L) {
           List<Embed> embedList = event.getMessage().getEmbeds();
            if (embedList.size() > 0 && embedList.get(0).getImage().isPresent() && embedList.get(0).getDescription().isPresent()) {
                DBBump.setNextBump(Instant.now().plus(2, ChronoUnit.HOURS));
                BumpReminder.getInstance().startCountdown(2 * 60 * 60 * 1000);
            }
        }

        return true;
    }

}