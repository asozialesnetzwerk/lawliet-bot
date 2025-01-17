package mysql.modules.fisheryusers;

import constants.FisheryCategoryInterface;
import core.DiscordApiManager;
import core.utils.NumberUtil;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.Optional;

public class FisheryUserPowerUpBean {

    private final long serverId, userId;
    private final int powerUpId;
    private int level;
    private final long startPrice, effect;
    private boolean changed = false;

    public FisheryUserPowerUpBean(long serverId, long userId, int powerUpId, int level) {
        this.serverId = serverId;
        this.userId = userId;
        this.powerUpId = powerUpId;
        this.level = level;
        this.startPrice = FisheryCategoryInterface.START_PRICE[powerUpId];
        this.effect = FisheryCategoryInterface.EFFECT[powerUpId];
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiManager.getInstance().getLocalServerById(serverId); }

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getServer().flatMap(server -> server.getMemberById(userId)); }

    public int getPowerUpId() { return powerUpId; }

    public int getLevel() { return level; }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }


    /* Setters */

    void setLevel(int level) {
        this.level = level;
        changed = true;
    }

    public void setChanged() {
        changed = true;
    }


    /* Tools */

    public long getPrice() {
        return NumberUtil.flattenLong(Math.round(Math.pow(getValue(level), 1.02) * startPrice), 4);
    }

    public long getEffect() {
        return getValue(level) * effect;
    }

    public long getDeltaEffect() {
        return (getValue(level + 1) - getValue(level)) * effect;
    }

    public static long getValue(long level) {
        long n = level + 1;
        return n * (n + 1) / 2;
    }

}
