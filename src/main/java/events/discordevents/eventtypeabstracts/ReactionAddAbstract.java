package events.discordevents.eventtypeabstracts;

import events.discordevents.DiscordEventAbstract;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.ArrayList;

public abstract class ReactionAddAbstract extends DiscordEventAbstract {

    public abstract boolean onReactionAdd(ReactionAddEvent event) throws Throwable;

    public static void onReactionAddStatic(ReactionAddEvent event, ArrayList<DiscordEventAbstract> listenerList) {
        if ((event.getMessage().isEmpty() && !event.getChannel().canYouReadMessageHistory()) ||
                event.getUser().isEmpty()
        ) {
            return;
        }

        execute(listenerList, event.getUser().get(), false, event.getServer().map(DiscordEntity::getId).orElse(0L),
                listener -> ((ReactionAddAbstract) listener).onReactionAdd(event)
        );
    }

}
