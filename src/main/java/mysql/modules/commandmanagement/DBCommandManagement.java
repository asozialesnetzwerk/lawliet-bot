package mysql.modules.commandmanagement;

import mysql.DBBeanGenerator;
import mysql.DBDataLoad;
import mysql.DBMain;

import java.sql.SQLException;
import java.util.ArrayList;

public class DBCommandManagement extends DBBeanGenerator<Long, CommandManagementBean> {

    private static final DBCommandManagement ourInstance = new DBCommandManagement();

    public static DBCommandManagement getInstance() {
        return ourInstance;
    }

    private DBCommandManagement() {
    }

    @Override
    protected CommandManagementBean loadBean(Long serverId) throws Exception {
        CommandManagementBean commandManagementBean = new CommandManagementBean(
                serverId,
                getSwitchedOffElements(serverId)
        );

        commandManagementBean.getSwitchedOffElements()
                .addListAddListener(list -> list.forEach(element -> addSwitchedOffElement(commandManagementBean.getServerId(), element)))
                .addListRemoveListener(list -> list.forEach(element -> removeSwitchedOffElement(commandManagementBean.getServerId(), element)));

        return commandManagementBean;
    }

    @Override
    protected void saveBean(CommandManagementBean commandManagementBean) {
    }

    private ArrayList<String> getSwitchedOffElements(long serverId) throws SQLException {
        return new DBDataLoad<String>("CMOff", "element", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> resultSet.getString(1));
    }

    private void addSwitchedOffElement(long serverId, String element) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO CMOff (serverId, element) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, element);
        });
    }

    private void removeSwitchedOffElement(long serverId, String element) {
        DBMain.getInstance().asyncUpdate("DELETE FROM CMOff WHERE serverId = ? AND element = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setString(2, element);
        });
    }

}
