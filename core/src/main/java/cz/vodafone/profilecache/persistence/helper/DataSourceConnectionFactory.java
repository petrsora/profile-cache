package cz.vodafone.profilecache.persistence.helper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceConnectionFactory implements ConnectionFactory {

    private DataSource dataSource;

    public DataSourceConnectionFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
