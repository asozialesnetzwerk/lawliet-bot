package mysql.modules.server;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.FisheryStatus;
import constants.Locales;
import core.DiscordApiManager;
import core.ResourceHandler;
import mysql.DBBeanGenerator;
import mysql.DBKeySetLoad;
import mysql.DBMain;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class DBServer extends DBBeanGenerator<Long, ServerBean> {

    private static final DBServer ourInstance = new DBServer();

    public static DBServer getInstance() {
        return ourInstance;
    }

    private DBServer() {
    }

    private final Cache<Long, Boolean> removedServerIds = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    @Override
    protected ServerBean loadBean(Long serverId) throws Exception {
        int shard = DiscordApiManager.getInstance().getResponsibleShard(serverId);
        if (shard < DiscordApiManager.getInstance().getShardIntervalMin() || shard > DiscordApiManager.getInstance().getShardIntervalMax())
            throw new Exception("Invalid server");

        boolean serverPresent = DiscordApiManager.getInstance().getLocalServerById(serverId).isPresent();
        if (serverPresent) {
            removedServerIds.invalidate(serverId);
        }

        ServerBean serverBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit FROM DServer WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            serverBean = new ServerBean(
                    serverId,
                    resultSet.getString(1),
                    new Locale(resultSet.getString(2)),
                    FisheryStatus.valueOf(resultSet.getString(3)),
                    resultSet.getBoolean(4),
                    resultSet.getLong(5),
                    resultSet.getBoolean(6),
                    resultSet.getBoolean(7),
                    resultSet.getLong(8),
                    resultSet.getLong(9),
                    resultSet.getInt(10),
                    resultSet.getBoolean(11),
                    resultSet.getBoolean(12)

            );
        } else {
            serverBean = new ServerBean(
                    serverId,
                    "L.",
                    new Locale(Locales.EN),
                    FisheryStatus.STOPPED,
                    false,
                    null,
                    true,
                    true,
                    50000,
                    800000000,
                    0,
                    false,
                    true
            );
            insertBean(serverBean);
        }

        resultSet.close();
        preparedStatement.close();

        return serverBean;
    }

    public boolean containsServerId(long serverId) {
        return !removedServerIds.asMap().containsKey(serverId);
    }

    private void insertBean(ServerBean serverBean) throws SQLException, InterruptedException {
        DBMain.getInstance().update("INSERT INTO DServer (serverId, prefix, locale, powerPlant, powerPlantSingleRole, powerPlantAnnouncementChannelId, powerPlantTreasureChests, powerPlantReminders, powerPlantRoleMin, powerPlantRoleMax, powerPlantVCHoursCap, commandAuthorMessageRemove, fisheryCoinsGivenLimit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverBean.getServerId());
            preparedStatement.setString(2, serverBean.getPrefix());
            preparedStatement.setString(3, serverBean.getLocale().getDisplayName());
            preparedStatement.setString(4, serverBean.getFisheryStatus().name());
            preparedStatement.setBoolean(5, serverBean.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = serverBean.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) preparedStatement.setLong(6, announcementChannelIdOpt.get());
            else preparedStatement.setNull(6, Types.BIGINT);

            preparedStatement.setBoolean(7, serverBean.isFisheryTreasureChests());
            preparedStatement.setBoolean(8, serverBean.isFisheryReminders());
            preparedStatement.setLong(9, serverBean.getFisheryRoleMin());
            preparedStatement.setLong(10, serverBean.getFisheryRoleMax());

            Optional<Integer> VCHoursOpt = serverBean.getFisheryVcHoursCap();
            if (VCHoursOpt.isPresent()) preparedStatement.setInt(11, VCHoursOpt.get());
            else preparedStatement.setNull(11, Types.INTEGER);

            preparedStatement.setBoolean(12, serverBean.isCommandAuthorMessageRemove());
            preparedStatement.setBoolean(13, serverBean.hasFisheryCoinsGivenLimit());
        });
    }

    @Override
    protected void saveBean(ServerBean serverBean) {
        DBMain.getInstance().asyncUpdate("UPDATE DServer SET prefix = ?, locale = ?, powerPlant = ?, powerPlantSingleRole = ?, powerPlantAnnouncementChannelId = ?, powerPlantTreasureChests = ?, powerPlantReminders = ?, powerPlantRoleMin = ?, powerPlantRoleMax = ?, powerPlantVCHoursCap = ?, commandAuthorMessageRemove = ?, fisheryCoinsGivenLimit = ? WHERE serverId = ?;", preparedStatement -> {
            preparedStatement.setLong(11, serverBean.getServerId());

            preparedStatement.setString(1, serverBean.getPrefix());
            preparedStatement.setString(2, serverBean.getLocale().getDisplayName());
            preparedStatement.setString(3, serverBean.getFisheryStatus().name());
            preparedStatement.setBoolean(4, serverBean.isFisherySingleRoles());

            Optional<Long> announcementChannelIdOpt = serverBean.getFisheryAnnouncementChannelId();
            if (announcementChannelIdOpt.isPresent()) preparedStatement.setLong(5, announcementChannelIdOpt.get());
            else preparedStatement.setNull(5, Types.BIGINT);

            preparedStatement.setBoolean(6, serverBean.isFisheryTreasureChests());
            preparedStatement.setBoolean(7, serverBean.isFisheryReminders());
            preparedStatement.setLong(8, serverBean.getFisheryRoleMin());
            preparedStatement.setLong(9, serverBean.getFisheryRoleMax());

            Optional<Integer> VCHoursOpt = serverBean.getFisheryVcHoursCap();
            if (VCHoursOpt.isPresent()) preparedStatement.setInt(10, VCHoursOpt.get());
            else preparedStatement.setNull(10, Types.INTEGER);

            preparedStatement.setBoolean(11, serverBean.isCommandAuthorMessageRemove());
            preparedStatement.setBoolean(12, serverBean.hasFisheryCoinsGivenLimit());
            preparedStatement.setLong(13, serverBean.getServerId());
        });
    }

    public void remove(long serverId) {
        removedServerIds.put(serverId, true);
        DBMain.getInstance().asyncUpdate("DELETE FROM DServer WHERE serverId = ?;", preparedStatement -> preparedStatement.setLong(1, serverId));
        getCache().invalidate(serverId);

        File welcomeBackgroundFile = ResourceHandler.getFileResource(String.format("data/welcome_backgrounds/%d.png", serverId));
        if (welcomeBackgroundFile.exists()) {
            welcomeBackgroundFile.delete();
        }
    }

    public ArrayList<Long> getAllServerIds() throws SQLException {
        return new DBKeySetLoad<Long>("DServers", "serverId")
                .get(resultSet -> resultSet.getLong(1));
    }

}
