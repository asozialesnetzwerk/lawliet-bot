package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;

import java.util.ArrayList;

public abstract class ServerChannelDeleteAbstract extends DiscordEventAbstract {

    public abstract boolean onServerChannelDelete(ServerChannelDeleteEvent event) throws Throwable;

    public static void onServerChannelDeleteStatic(ServerChannelDeleteEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        execute(listenerList, true, event.getServer().getId(),
                listener -> ((ServerChannelDeleteAbstract) listener).onServerChannelDelete(event)
        );
    }

}
