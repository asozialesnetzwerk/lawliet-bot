package MySQL;

import ServerStuff.TopGG;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBBotStats {

    public static void addStatServers(int serverCount) throws SQLException {
        String sql = "INSERT INTO StatsServerCount VALUES(NOW(), ?);";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, serverCount);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void addStatCommandUsages() throws SQLException {
        String sql = "INSERT INTO StatsCommandUsages VALUES(NOW(), (SELECT SUM(usages) FROM CommandUsages));";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.execute();
        preparedStatement.close();
    }

    public static void addStatUpvotes() throws SQLException {
        String sql = "INSERT INTO StatsUpvotes VALUES(NOW(), ?, ?);";
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setInt(1, TopGG.getInstance().getTotalUpvotes());
        preparedStatement.setInt(2, TopGG.getInstance().getMonthlyUpvotes());
        preparedStatement.execute();
        preparedStatement.close();
    }

}