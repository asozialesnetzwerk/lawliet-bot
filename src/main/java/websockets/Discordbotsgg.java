package websockets;

import constants.AssetIds;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;

public class Discordbotsgg {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guildCount", serverCount);
        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("DISCORDBOTSGG_TOKEN"))
        };
        HttpRequest.getData(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", AssetIds.LAWLIET_USER_ID), jsonObject.toString(), properties).exceptionally(ExceptionLogger.get());
    }

}
