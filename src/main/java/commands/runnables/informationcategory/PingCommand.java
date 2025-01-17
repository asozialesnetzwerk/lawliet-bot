package commands.runnables.informationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.time.Instant;
import java.util.Locale;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        executableWithoutArgs = true
)
public class PingCommand extends Command {

    public PingCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Instant startTime = (Instant) getAttachments().get("starting_time");
        long milisInternal = TimeUtil.getMilisBetweenInstants(startTime, Instant.now());
        long milisGateway = event.getApi().getLatestGatewayLatency().toMillis();
        long milisRest = event.getApi().measureRestLatency().get().toMillis();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("pong",
                StringUtil.numToString(milisInternal),
                StringUtil.numToString(milisGateway),
                StringUtil.numToString(milisRest)
        ));
        event.getChannel().sendMessage(eb).get();

        return true;
    }

}