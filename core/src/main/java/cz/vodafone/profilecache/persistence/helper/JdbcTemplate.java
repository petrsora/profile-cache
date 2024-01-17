package cz.vodafone.profilecache.persistence.helper;

import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JdbcTemplate {

    private static final Logger LOG = Logger.getLogger(JdbcTemplate.class);

    private ConnectionFactory connectionFactory;

    public JdbcTemplate(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public Connection getConnection() throws SQLException {
        return this.connectionFactory.getConnection();
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.error("Error while closing connection", e);
            }
        }
    }

    public static void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOG.error("Error while performing rollback", e);
            }
        }
    }

    public <T> T queryOneRow(String sql, Mapper<T> mapper, Object... parameters) throws SQLException {
        List<T> result = query(sql, mapper, 1, parameters);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public <T> T queryOneRow(Connection connection, String sql, Mapper<T> mapper, Object... parameters) throws SQLException {
        List<T> result = query(connection, sql, mapper, 1, parameters);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public <T> List<T> queryAllRows(String sql, Mapper<T> mapper, Object... parameters) throws SQLException {
        return query(sql, mapper, Integer.MIN_VALUE, parameters);
    }

    public <T> List<T> queryAllRows(Connection connection, String sql, Mapper<T> mapper, Object... parameters) throws SQLException {
        return query(connection, sql, mapper, Integer.MIN_VALUE, parameters);
    }

    public <T> List<T> query(String sql, Mapper<T> mapper, int maxRows, Object... parameters) throws SQLException {
        Connection con = null;

        try {
            con = this.connectionFactory.getConnection();
            return query(con, sql, mapper, maxRows, parameters);
        } finally {
            closeConnection(con);
        }
    }

    public <T> List<T> query(Connection connection, String sql, Mapper<T> mapper, int maxRows, Object... parameters) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method query(con, %s, mapper, %d, parameters...)", sql, maxRows));
        }
        DbObjects dbObjects = null;
        try {
            dbObjects = query(connection, sql, parameters);

            List<T> result = new ArrayList<T>();
            int count = 0;
            T t;
            while ((t = nextRow(dbObjects, mapper)) != null) {
                if (maxRows != Integer.MIN_VALUE && count >= maxRows) {
                    break;
                }
                result.add(t);
                count++;
            }

            return result;

        } finally {
            closeDbObjects(dbObjects);
        }
    }

    public DbObjects query(Connection connection, String sql, Object... parameters) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method query(con, %s, parameters...)", sql));
        }
        PreparedStatement ps;
        ResultSet rs;

        ps = connection.prepareStatement(sql);

        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                Object param = parameters[i];
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    ps.setLong(i + 1, Long.valueOf((Integer) param));
                } else if (param instanceof Date) {
                    ps.setTimestamp(i + 1, new Timestamp(((Date) param).getTime()));
                } else {
                    throw new IllegalArgumentException("Unsupported parameter");
                }
            }
        }

        rs = ps.executeQuery();

        return new DbObjects(ps, rs);
    }

    public <T> T nextRow(DbObjects dbObjects, Mapper<T> mapper) throws SQLException {
        if (!dbObjects.getResultSet().next()) {
            return null;
        }
        return mapper.map(dbObjects.getResultSet());
    }

    public int update(Connection connection, String sql, Object... parameters) throws SQLException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method update(con, %s, parameters...)", sql));
        }
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = connection.prepareStatement(sql);

            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    Object param = parameters[i];
                    if (param instanceof String) {
                        ps.setString(i + 1, (String) param);
                    } else if (param instanceof Long) {
                        ps.setLong(i + 1, (Long) param);
                    } else if (param instanceof Integer) {
                        ps.setLong(i + 1, Long.valueOf((Integer) param));
                    } else if (param instanceof Date) {
                        ps.setTimestamp(i + 1, new Timestamp(((Date) param).getTime()));
                    } else {
                        throw new IllegalArgumentException("Unsupported parameter");
                    }
                }
            }

            return ps.executeUpdate();

        } finally {
            closeDbObjects(ps, rs);
        }
    }

    public static void closeDbObjects(DbObjects dbObjects) {
        if (dbObjects != null) {
            closeDbObjects(dbObjects.getPreparedStatement(), dbObjects.getResultSet());
        }
    }

    private static void closeDbObjects(PreparedStatement ps, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.error("Error while closing result set");
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                LOG.error("Error while closing prepared statement");
            }
        }
    }

}
