package mysql.modules.giveaway;

import core.DiscordApiManager;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Observable;
import java.util.Optional;

public class GiveawayBean extends Observable {

    private final long serverId;
    private final long messageId;
    private final long channelId;
    private final String emoji;
    private final int winners;
    private final Instant start;
    private final long durationMinutes;
    private final String title;
    private final String description;
    private final String imageUrl;
    private boolean active;

    public GiveawayBean(long serverId, long channelId, long messageId, String emoji, int winners, Instant start, long durationMinutes, String title, String description, String imageUrl, boolean active) {
        this.serverId = serverId;
        this.messageId = messageId;
        this.channelId = channelId;
        this.emoji = emoji;
        this.winners = winners;
        this.start = start;
        this.durationMinutes = durationMinutes;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.active = active || getEnd().isAfter(Instant.now());
    }

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() {
        return DiscordApiManager.getInstance().getLocalServerById(serverId);
    }

    public long getMessageId() {
        return messageId;
    }

    public Optional<ServerTextChannel> getChannel() {
        return getServer().flatMap(server -> server.getTextChannelById(channelId));
    }

    public long getChannelId() {
        return channelId;
    }

    public Optional<Message> getMessage() {
        return DiscordApiManager.getInstance().getMessageById(serverId, channelId, messageId).join();
    }

    public String getEmoji() {
        return emoji;
    }

    public int getWinners() {
        return winners;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return start.plus(durationMinutes, ChronoUnit.MINUTES);
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Optional<String> getImageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    public void stop() {
        active = false;
        setChanged();
        notifyObservers();
    }

    public boolean isActive() {
        return active;
    }

}
