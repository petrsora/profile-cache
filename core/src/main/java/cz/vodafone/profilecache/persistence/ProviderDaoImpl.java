package cz.vodafone.profilecache.persistence;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import cz.vodafone.profilecache.persistence.helper.ConnectionFactory;
import cz.vodafone.profilecache.persistence.helper.JdbcTemplate;
import cz.vodafone.profilecache.persistence.helper.Mapper;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ProviderDaoImpl implements ProviderDao {

    private static final Logger LOG = Logger.getLogger(ProviderDaoImpl.class);

    public ProviderDaoImpl(ConnectionFactory connectionFactory) {
        jdbcTemplate = new JdbcTemplate(connectionFactory);
    }

    private JdbcTemplate jdbcTemplate;

    private static final class IntegerMapper implements Mapper<Integer> {

        public Integer map(ResultSet rs) throws SQLException {
            return rs.getInt(1);
        }

    }

    @Override
    public Profile getProfile(String msisdn) throws ProviderDaoException {
        long ts = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getProfile started (%s) ...", msisdn));
        }
        try {
            MsisdnList msisdnList = jdbcTemplate.queryOneRow(
                    "select ID,MSISDN from MSISDN_LIST where MSISDN=?", new MsisdnListMapper(), msisdn);
            if (msisdnList == null) {
                return null;
            }

            List<AttributeList> attributes = jdbcTemplate.queryAllRows(
                    "select ID,ATTRIBUTE_NAME,LAST_UPDATE,EVENT_ID,ATTRIBUTE_VALUE from ATTRIBUTE_LIST where ID=?",
                    new AttributeListMapper(), msisdnList.getId());

            ProfileImpl.Builder builder = new ProfileImpl.Builder().
                    setMsisdn(msisdnList.getMsisdn()).
                    setOperatorId(Location.OPERATOR_ID_VFCZ). // in DB should be only VFCZ subscriber
                    setId(msisdnList.getId());
            for (AttributeList attribute : attributes) {
                Attribute attr = new AttributeImpl(attribute.getName(), attribute.getValue(), attribute.getLastUpdate());
                builder.setAttribute(attr);
            }
            LOG.info(String.format("SYS=DB OP=GET-PROFILE RES=SUCCESS RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), msisdn));
            return builder.build();
        } catch (SQLException e) {
            LOG.error(String.format("SYS=DB OP=GET-PROFILE RES=ERROR RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), msisdn));
            throw new ProviderDaoException("Error while getting profile from DB", e);
        }
    }

    @Override
    public Profile insertProfile(Profile profile) throws ProviderDaoException {
        long ts = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method insertProfile started (%s) ...", profile.getMsisdn()));
        }
        Connection con = null;
        try {
            con = jdbcTemplate.getConnection();
            con.setAutoCommit(false);

            Integer id = jdbcTemplate.queryOneRow(con, "select MSISDN_LIST_ID_SEQ.NEXTVAL NEXTVAL from DUAL", new IntegerMapper());
            if (id == null) {
                throw new ProviderDaoException("Not available NEXTVAL in MSISDN_LIST_ID_SEQ sequence");
            }

            profile.setId(id);
            jdbcTemplate.update(con, "insert into MSISDN_LIST (ID,MSISDN) values (?, ?)", id, profile.getMsisdn());

            insertAttributes(con, profile);
            con.commit();

            LOG.info(String.format("SYS=DB OP=INSERT-PROFILE RES=SUCCESS RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
            return profile;
        } catch (SQLException e) {
            JdbcTemplate.rollback(con);
            LOG.error(String.format("SYS=DB OP=INSERT-PROFILE RES=ERROR RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
            throw new ProviderDaoException("Error while deleting profile", e);
        } finally {
            JdbcTemplate.closeConnection(con);
        }
    }

    @Override
    public void deleteProfile(Profile profile) throws ProviderDaoException {
        long ts = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method deleteProfile started (%s) ...", profile.getMsisdn()));
        }
        Connection con = null;
        try {
            con = jdbcTemplate.getConnection();
            con.setAutoCommit(false);

            int deletedRows = deleteAttributes(con, profile);
            if (deletedRows == 0) {
                LOG.warn(String.format("No attributes deleted while deleting complete profile (%s)", profile.getMsisdn()));
            }

            deletedRows = jdbcTemplate.update(con,
                    "delete from MSISDN_LIST where MSISDN=?", profile.getMsisdn());
            if (deletedRows == 0) {
                LOG.warn(String.format("No msisdn deleted while deleting complete profile (%s)", profile.getMsisdn()));
            }
            con.commit();

            LOG.info(String.format("SYS=DB OP=DELETE-PROFILE RES=SUCCESS RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
        } catch (SQLException e) {
            JdbcTemplate.rollback(con);

            LOG.error(String.format("SYS=DB OP=DELETE-PROFILE RES=ERROR RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
            throw new ProviderDaoException("Error while deleting profile", e);
        } finally {
            JdbcTemplate.closeConnection(con);
        }
    }

    @Override
    public void updateAttributes(Profile profile) throws ProviderDaoException {
        long ts = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method updateAttributes started (%s) ...", profile.getMsisdn()));
        }
        Connection con = null;
        try {
            con = jdbcTemplate.getConnection();
            con.setAutoCommit(false);

            if (profile.getId() == null) {
                MsisdnList msisdnList = jdbcTemplate.queryOneRow(
                        con, "select ID,MSISDN from MSISDN_LIST where MSISDN=?", new MsisdnListMapper(), profile.getMsisdn());
                if (msisdnList == null) {
                    throw new ProviderDaoException("Profile does not exist in DB");
                }
                profile.setId(msisdnList.getId());
            }

            int deletedRows = deleteAttributes(con, profile);
            if (deletedRows == 0) {
                LOG.warn(String.format("No attributes deleted while updating attributes (%s)", profile.getMsisdn()));
            }

            insertAttributes(con, profile);
            con.commit();

            LOG.info(String.format("SYS=DB OP=UPDATE-ATTRIBUTES RES=SUCCESS RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
        } catch (SQLException e) {
            JdbcTemplate.rollback(con);
            LOG.error(String.format("SYS=DB OP=UPDATE-ATTRIBUTES RES=ERROR RT=%d MSISDN=%s", (System.currentTimeMillis() - ts), profile.getMsisdn()));
            throw new ProviderDaoException("Error while updating attributes", e);
        } finally {
            JdbcTemplate.closeConnection(con);
        }
    }

    @Override
    public boolean updateMsisdn(String oldMsisdn, String newMsisdn) throws ProviderDaoException {
        long ts = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method updateMsisdn started (%s,%s) ...", oldMsisdn, newMsisdn));
        }
        Connection con = null;
        try {
            con = jdbcTemplate.getConnection();
            con.setAutoCommit(false);

            int rowCount = jdbcTemplate.update(con, "update MSISDN_LIST set MSISDN=? where MSISDN=?", newMsisdn, oldMsisdn);
            con.commit();

            LOG.info(String.format("SYS=DB OP=UPDATE-MSISDN RES=SUCCESS RT=%d OLD-MSISDN=%s NEW-MSISDN=%s",
                    (System.currentTimeMillis() - ts), oldMsisdn, newMsisdn));
            return (rowCount == 1);
        } catch (SQLException e) {
            JdbcTemplate.rollback(con);
            LOG.error(String.format("SYS=DB OP=UPDATE-MSISDN RES=ERROR RT=%d OLD-MSISDN=%s NEW-MSISDN=%s",
                    (System.currentTimeMillis() - ts), oldMsisdn, newMsisdn));
            throw new ProviderDaoException("Error while updating MSISDN", e);
        } finally {
            JdbcTemplate.closeConnection(con);
        }
    }

    private int deleteAttributes(Connection con, Profile profile) throws SQLException {
        if (profile.getId() == null) {
            return jdbcTemplate.update(con,
                    "delete from ATTRIBUTE_LIST where ID=(select ID from MSISDN_LIST where MSISDN=?)", profile.getMsisdn());
        } else {
            return jdbcTemplate.update(con,
                    "delete from ATTRIBUTE_LIST where ID=?", profile.getId());
        }
    }

    private void insertAttributes(Connection con, Profile profile) throws SQLException {
        for (Attribute attribute : profile.getAttributes()) {
            insertAttribute(con, profile.getId(), attribute.getName(), attribute.getValue(), attribute.getLastUpdate(), 1);
        }
    }

    private void insertAttribute(Connection con, Integer id, String name, String value, Date lastUpdate, Integer eventId) throws SQLException {
        jdbcTemplate.update(con,
                "insert into ATTRIBUTE_LIST (ID, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, LAST_UPDATE, EVENT_ID) VALUES (?, ?, ?, ?, ?)",
                id, name, value, lastUpdate, eventId);
    }

}
