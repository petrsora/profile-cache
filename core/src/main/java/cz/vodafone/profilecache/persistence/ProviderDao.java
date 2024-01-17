package cz.vodafone.profilecache.persistence;

import cz.vodafone.profilecache.model.Profile;

public interface ProviderDao {

    public Profile getProfile(String msisdn) throws ProviderDaoException;

    public Profile insertProfile(Profile profile) throws ProviderDaoException;

    public void deleteProfile(Profile profile) throws ProviderDaoException;

    /**
     * deletes all current attributes and insert new attributes from newProfile
     * @param profile
     * @throws ProviderDaoException
     */
    public void updateAttributes(Profile profile) throws ProviderDaoException;

    public boolean updateMsisdn(String oldMsisdn, String newMsisdn) throws ProviderDaoException;
}
