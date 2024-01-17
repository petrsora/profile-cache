package cz.vodafone.profilecache.persistence.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class RawConnectionFactory implements ConnectionFactory {

    private String driver;
    private String url;
    private String username;
    private String password;

    public RawConnectionFactory(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(this.driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Missing driver " + this.driver);
        }
        return DriverManager.getConnection(this.url, this.username, this.password);
    }

}
