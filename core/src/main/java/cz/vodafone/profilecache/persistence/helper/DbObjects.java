package cz.vodafone.profilecache.persistence.helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbObjects {

    private PreparedStatement preparedStatement;

    private ResultSet resultSet;

    public DbObjects(PreparedStatement preparedStatement, ResultSet resultSet) {
        this.preparedStatement = preparedStatement;
        this.resultSet = resultSet;
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }
}
