package mysql.modules.autoquote;

import mysql.DBBeanGenerator;
import mysql.DBMain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBAutoQuote extends DBBeanGenerator<Long, AutoQuoteBean> {

    private static final DBAutoQuote ourInstance = new DBAutoQuote();

    public static DBAutoQuote getInstance() {
        return ourInstance;
    }

    private DBAutoQuote() {
    }

    @Override
    protected AutoQuoteBean loadBean(Long serverId) throws Exception {
        AutoQuoteBean autoQuoteBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT active FROM AutoQuote WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            autoQuoteBean = new AutoQuoteBean(
                    serverId,
                    resultSet.getBoolean(1)
            );
        } else {
            autoQuoteBean = new AutoQuoteBean(
                    serverId,
                    true
            );
        }

        resultSet.close();
        preparedStatement.close();

        return autoQuoteBean;
    }

    @Override
    protected void saveBean(AutoQuoteBean serverBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO AutoQuote (serverId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverBean.getServerId());
            preparedStatement.setBoolean(2, serverBean.isActive());
        });
    }

}
