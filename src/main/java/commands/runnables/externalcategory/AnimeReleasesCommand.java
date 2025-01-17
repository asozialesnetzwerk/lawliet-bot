package commands.runnables.externalcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.PostBundle;
import modules.animerelease.AnimeReleaseDownloader;
import modules.animerelease.AnimeReleasePost;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
        trigger = "crunchyroll",
        withLoadingBar = true,
        emoji = "\uD83D\uDCFA",
        executableWithoutArgs = true,
        aliases = { "animereleases", "animerelease" }
)
public class AnimeReleasesCommand extends Command implements OnTrackerRequestListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(AnimeReleasesCommand.class);

    public AnimeReleasesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        PostBundle<AnimeReleasePost> posts = AnimeReleaseDownloader.getPosts(getLocale(), null, followedString);

        if (posts.getPosts().size() > 0) {
            EmbedBuilder eb = EmbedUtil.addTrackerNoteLog(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), getEmbed(posts.getPosts().get(0)), getPrefix(), getTrigger());
            event.getChannel().sendMessage(eb).get();
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", false, followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }

    private EmbedBuilder getEmbed(AnimeReleasePost post) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(post.getAnime(), post.getUrl(), "https://cdn.discordapp.com/attachments/499629904380297226/782242962201116723/crunchyroll_favicon.png")
                .setDescription(post.getDescription())
                .setUrl(post.getUrl())
                .setImage(post.getThumbnail())
                .setTimestamp(post.getInstant());
        EmbedUtil.setFooter(eb, this);

        if (post.getEpisode().isPresent()) {
            if (post.getEpisodeTitle().isPresent()) eb.setTitle(getString("template_title",post.getEpisode().get(), post.getEpisodeTitle().get()));
            else eb.setTitle(getString("template_title_bundle", post.getEpisode().get()));
        } else {
            if (post.getEpisodeTitle().isPresent()) eb.setTitle(post.getEpisodeTitle().get());
        }

        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        boolean first = slot.getArgs().isEmpty();
        PostBundle<AnimeReleasePost> postBundle = AnimeReleaseDownloader.getPosts(getLocale(), slot.getArgs().orElse(null), slot.getCommandKey());

        ServerTextChannel channel = slot.getChannel().get();
        for(int i = Math.min(4, postBundle.getPosts().size() - 1); i >= 0; i--) {
            AnimeReleasePost post = postBundle.getPosts().get(i);
            channel.sendMessage(getEmbed(post)).get();
        }

        if (first && postBundle.getPosts().size() == 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .setDescription(getString("no_results", true, StringUtil.shortenString(slot.getCommandKey(), 200)));
            slot.getChannel().get().sendMessage(eb).get();
        }

        if (postBundle.getNewestPost() != null) slot.setArgs(postBundle.getNewestPost());
        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}