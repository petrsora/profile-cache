package cz.vodafone.profilecache.bulkupload.impl;

import cz.vodafone.profilecache.bulkupload.BulkUploadClientException;
import cz.vodafone.profilecache.bulkupload.BulkUploadDAO;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.persistence.helper.RawConnectionFactory;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Created by: xpetsora
 * Date: 25.10.2013
 */
public class BulkUploadDAOImpl implements BulkUploadDAO {
    private static final Logger LOG = Logger.getLogger(BulkUploadDAOImpl.class);
    private static final String CALLABLE_UPSERT = "{ CALL PROFILE_OWN.PROF_CACHE_PKG.UPSERT_PROFILE( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";

    private long commitQueueSize;

    Connection connection;
    CallableStatement st_upsert;

    public BulkUploadDAOImpl() throws BulkUploadClientException {
        try {
            commitQueueSize = 0;
            RawConnectionFactory connectionFactory = new RawConnectionFactory(Configuration.getMandatoryString(ConfigurationItems.DB_DRIVER),
                    Configuration.getMandatoryString(ConfigurationItems.DB_URL),
                    Configuration.getMandatoryString(ConfigurationItems.DB_USERNAME),
                    Configuration.getMandatoryString(ConfigurationItems.DB_PASSWORD));
            connection = connectionFactory.getConnection();
            connection.setAutoCommit(false);

            st_upsert = connection.prepareCall(CALLABLE_UPSERT);
            st_upsert.setQueryTimeout(1);
            st_upsert.registerOutParameter(10, Types.NUMERIC);
        } catch (SQLException e) {
            throw new BulkUploadClientException(String.format("Error when initializing DAO (%s)", e.getMessage()), e);
        }
    }

    @Override
    public String upsertProfile(Profile profile) throws BulkUploadClientException {
        String pRet;
        List<Attribute> attrs = profile.getAttributes();
        try {
            st_upsert.setString(1, profile.getMsisdn());
            int indexSum = 0;
            for (Attribute attr : attrs) {
                String attrName = attr.getName();
                int index = 0;
                if (Profile.ATTR_IS_PREPAID.equalsIgnoreCase(attrName)) {
                    index = 2;
                }
                if (Profile.ATTR_IS_CHILD.equalsIgnoreCase(attrName)) {
                    index = 3;
                }
                if (Profile.ATTR_IS_RESTRICTED.equalsIgnoreCase(attrName)) {
                    index = 4;
                }
                if (Profile.ATTR_HAS_PRSMS_BARRING.equalsIgnoreCase(attrName)) {
                    index = 5;
                }
                if (Profile.ATTR_SCHEDULING_PROFILE.equalsIgnoreCase(attrName)) {
                    index = 6;
                }
                if (Profile.ATTR_LOCATION.equalsIgnoreCase(attrName)) {
                    index = 7;
                }
                if (Profile.ATTR_HAS_MPENEZENKA_BARRING.equalsIgnoreCase(attrName)) {
                    index = 8;
                }
                if (Profile.ATTR_IS_VOLTE.equalsIgnoreCase(attrName)) {
                    index = 9;
                }
                indexSum += index;
                //if (LOG.isDebugEnabled()) { LOG.debug("Attribute " + attrName + " index determined " + index); }
                String attrVal = attr.getValue();
                //is prepaid / is child / has_prsms / has_mpenebar / is_volte conversion '1'->'T'
                if (index == 2 || index == 3 || index == 5 || index == 8 || index == 9) {
                    if (attrVal != null && !attrVal.isEmpty()) {
                        if ("1".equals(attrVal) || Profile.ATTR_VALUE_TRUE.equalsIgnoreCase(attrVal)) {
                            attrVal = Profile.ATTR_VALUE_TRUE;
                        } else {
                            attrVal = Profile.ATTR_VALUE_FALSE;
                        }
                    }
                }
                st_upsert.setString(index, attrVal);
            }
            // chk whether all indexes (2..9) were assigned
            if (indexSum != 44) {
                LOG.error("Some attribute was not assigned! indexSum = " + indexSum);
            }

            long startTime = System.currentTimeMillis();
            st_upsert.executeQuery();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Callable command executed. Elapsed time: " + (System.currentTimeMillis() - startTime));
            }
            commitQueueSize++;
            pRet = String.valueOf(st_upsert.getInt(10));
        } catch (SQLException e) {
            throw new BulkUploadClientException(
                    String.format("upsertProfile() failed at %d uncommitted transactions! ", commitQueueSize), e);
        }
        return pRet;
    }

    @Override
    public void commit() throws BulkUploadClientException {
        try {
            long startTime = System.currentTimeMillis();
            connection.commit();
            commitQueueSize = 0;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Commit issued. Elapsed time: " + (System.currentTimeMillis() - startTime));
            }
        } catch (SQLException e) {
            throw new BulkUploadClientException(String.format("Error issuing commit (%s)", e.getMessage()), e);
        }
    }

    @Override
    public void close() {
        try {
            if (st_upsert != null) {
                st_upsert.close();
            }
        } catch (SQLException e) {
            LOG.error(String.format("Error while disposing prepared statements! (%s)", e.getMessage()), e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.error(String.format("Error while closing connections! (%s)", e.getMessage()), e);
            }
        }
    }

}
