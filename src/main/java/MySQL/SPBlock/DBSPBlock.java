package MySQL.SPBlock;

import MySQL.DBBeanGenerator;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.Server.DBServer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBSPBlock extends DBBeanGenerator<Long, SPBlockBean> {

    private static DBSPBlock ourInstance = new DBSPBlock();
    public static DBSPBlock getInstance() { return ourInstance; }
    private DBSPBlock() {}

    @Override
    protected SPBlockBean loadBean(Long serverId) throws Exception {
        SPBlockBean spBlockBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active, action FROM SPBlock WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            spBlockBean = new SPBlockBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getBoolean(1),
                    SPBlockBean.ActionList.valueOf(resultSet.getString(2)),
                    getIgnoredUsers(serverId),
                    getIgnoredChannels(serverId),
                    getLogReceivers(serverId)
            );
        } else {
            spBlockBean = new SPBlockBean(
                    serverId,
                    DBServer.getInstance().getBean(serverId),
                    false,
                    SPBlockBean.ActionList.DELETE_MESSAGE,
                    getIgnoredUsers(serverId),
                    getIgnoredChannels(serverId),
                    getLogReceivers(serverId)
            );
        }

        resultSet.close();
        preparedStatement.close();

        spBlockBean.getIgnoredUserIds()
                .addListAddListener(list -> list.forEach(userId -> addIgnoredUser(serverId, userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeIgnoredUser(serverId, userId)));
        spBlockBean.getLogReceiverUserIds()
                .addListAddListener(list -> list.forEach(userId -> addLogReceiver(serverId, userId)))
                .addListRemoveListener(list -> list.forEach(userId -> removeLogReceiver(serverId, userId)));
        spBlockBean.getIgnoredChannelIds()
                .addListAddListener(list -> list.forEach(channelId -> addIgnoredChannels(serverId, channelId)))
                .addListRemoveListener(list -> list.forEach(channelId -> removeIgnoredChannels(serverId, channelId)));
        return spBlockBean;
    }

    @Override
    protected void saveBean(SPBlockBean spBlockBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO SPBlock (serverId, active, action) VALUES (?, ?, ?);");
        preparedStatement.setLong(1, spBlockBean.getServerId());
        preparedStatement.setBoolean(2, spBlockBean.isActive());
        preparedStatement.setString(3, spBlockBean.getAction().name());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    private ArrayList<Long> getIgnoredUsers(long serverId) throws SQLException {
        return new DBDataLoad<Long>("SPBlockIgnoredUsers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredUser(long serverId, long userId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO SPBlockIgnoredUsers (serverId, userId) VALUES (?, ?);");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeIgnoredUser(long serverId, long userId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM SPBlockIgnoredUsers WHERE serverId = ? AND userId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Long> getLogReceivers(long serverId) throws SQLException {
        return new DBDataLoad<Long>("SPBlockLogRecievers", "userId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addLogReceiver(long serverId, long userId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO SPBlockLogRecievers (serverId, userId) VALUES (?, ?);");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeLogReceiver(long serverId, long userId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM SPBlockLogRecievers WHERE serverId = ? AND userId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, userId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Long> getIgnoredChannels(long serverId) throws SQLException {
        return new DBDataLoad<Long>("SPBlockIgnoredChannels", "channelId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addIgnoredChannels(long serverId, long channelId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO SPBlockIgnoredChannels (serverId, channelId) VALUES (?, ?);");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeIgnoredChannels(long serverId, long channelId) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM SPBlockIgnoredChannels WHERE serverId = ? AND channelId = ?;");
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, channelId);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
