package Commands.Splatoon2Category;

import CommandListeners.CommandProperties;

import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import General.*;
import General.Internet.InternetCache;
import General.Tools.TimeTools;
import General.Tracker.TrackerData;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Date;

@CommandProperties(
    trigger = "splatnet",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    withLoadingBar = true,
    emoji = "\uD83D\uDED2",
    thumbnail = "https://vignette.wikia.nocookie.net/splatoon/images/1/12/InklingUsingSplatNet.jpg/revision/latest?cb=20160116221000&path-prefix=de",
    executable = true
)
public class SplatnetCommand extends Command implements onTrackerRequestListener {
    
    private Instant trackingTime;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        int datesShown = 2;
        String language = getLocale().getLanguage().split("_")[0].toLowerCase();

        String[] urls = new String[]{
                "https://splatoon2.ink/data/merchandises.json",
                "https://splatoon2.ink/data/locale/" + language + ".json"
        };

        JSONArray netData = new JSONObject(InternetCache.getData(urls[0]).get().getContent().get()).getJSONArray("merchandises");
        JSONObject languageData = new JSONObject(InternetCache.getData(urls[1]).get().getContent().get());;

        //Sorgt dafür, dass die aktuellen Daten genommen werden.
        if (netData.length() == 6) {
            for (int i = 0; i < netData.length(); i++) {
                JSONObject data = netData.getJSONObject(i);
                Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();
                if (endTime.isBefore(Instant.now())) {
                    Thread.sleep(5000);
                    return getEmbed();
                }
            }
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setTimestampToNow();

        Instant trackingTime = null;
        for(int i=0; i < netData.length(); i++) {
            JSONObject data = netData.getJSONObject(i);
            Instant endTime = new Date(data.getLong("end_time") * 1000L).toInstant();

            if (trackingTime == null) trackingTime = endTime;
            if (endTime.isBefore(trackingTime)) trackingTime = endTime;

            String gearName = languageData.getJSONObject("gear").getJSONObject(data.getString("kind")).getJSONObject(data.getJSONObject("gear").getString("id")).getString("name");
            String fieldTitle = DiscordApiCollection.getInstance().getHomeEmojiById(437258157136543744L).getMentionTag() + " __**" + gearName + "**__";
            int price = data.getInt("price");

            String mainAbility = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("skill").getString("id")).getString("name");
            int slots = data.getJSONObject("gear").getInt("rarity") + 1;
            String brand = languageData.getJSONObject("brands").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getString("id")).getString("name");
            //String effect = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getJSONObject("frequent_skill").getString("id")).getString("name");
            String effect = getString("nothing");
            if (data.getJSONObject("gear").getJSONObject("brand").has("frequent_skill")) effect = languageData.getJSONObject("skills").getJSONObject(data.getJSONObject("gear").getJSONObject("brand").getJSONObject("frequent_skill").getString("id")).getString("name");

            String fieldContent = getString("template", DiscordApiCollection.getInstance().getHomeEmojiById(437239777834827786L).getMentionTag(), String.valueOf(price), TimeTools.getInstantString(getLocale(), endTime, true), TimeTools.getRemainingTimeString(getLocale(), endTime, Instant.now(), true), mainAbility, String.valueOf(slots), brand, effect);
            eb.addField(fieldTitle, fieldContent, true);
        }

        this.trackingTime = trackingTime;

        InternetCache.setExpirationDate(trackingTime, urls);
        return eb;
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        trackerData.deletePreviousMessage();
        Message message = trackerData.getChannel().get().sendMessage(getEmbed()).get();
        trackerData.setMessageDelete(message);
        trackerData.setInstant(trackingTime);
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
