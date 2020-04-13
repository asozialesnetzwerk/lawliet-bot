package ServerStuff.WebCommunicationServer.Events;

import Constants.FisheryStatus;
import Core.DiscordApiCollection;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Upvotes.DBUpvotes;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class OnTopGG implements DataListener<JSONObject> {

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        long userId = jsonObject.getLong("user");
        String type = jsonObject.getString("type");
        boolean isWeekend = jsonObject.getBoolean("isWeekend");

        if (type.equals("upvote")) {
            DiscordApiCollection.getInstance().getUserById(userId).ifPresent(user -> {
                System.out.println("UPVOTE | " + user.getName());

                DiscordApiCollection.getInstance().getMutualServers(user).stream().filter(
                        server -> {
                            try {
                                return DBServer.getInstance().getBean(server.getId()).getFisheryStatus() == FisheryStatus.ACTIVE;
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            return false;
                        }
                ).forEach(server -> {
                    try {
                        DBFishery.getInstance().getBean(server.getId()).getUser(userId).addUpvote(isWeekend ? 2 : 1);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            });
            DBUpvotes.getInstance().getBean(userId).updateLastUpvote();

            //Send data
            socketIOClient.sendEvent(WebComServer.EVENT_TOPGG);
        } else {
            System.out.println("Wrong type: " + type);
        }
    }

}