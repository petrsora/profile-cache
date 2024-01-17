package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.builder.ProfileBuilder;
import cz.vodafone.profilecache.builder.ProfileBuilderException;
import cz.vodafone.profilecache.cache.Cache;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.LocationProvider;
import cz.vodafone.profilecache.location.LocationProviderException;
import cz.vodafone.profilecache.maintenance.Status;
import cz.vodafone.profilecache.model.*;
import cz.vodafone.profilecache.persistence.ProviderDao;
import cz.vodafone.profilecache.persistence.ProviderDaoException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ProfileProviderImpl implements ProfileProvider {

    private static final Logger LOG = Logger.getLogger(ProfileProviderImpl.class);

    private static final String ORA_00001_UNIQUE_CONSTRAINT_VIOLATION = "ORA-00001";

    private MsisdnValidator msisdnValidator;
    private ImsiValidator imsiValidator;
    private Cache cache;
    private LocationProvider locationProvider;
    private ProviderDao providerDao;
    private ProfileBuilder profileBuilder;
    private long locationExpiryPeriod; // default is one hour
    private boolean dbOfflineMode;
    private Status status;

    public static class Builder {
        private MsisdnValidator msisdnValidator;
        private ImsiValidator imsiValidator;
        private Cache cache;
        private LocationProvider locationProvider;
        private ProviderDao providerDao;
        private ProfileBuilder profileBuilder;
        private long locationExpiryPeriod = 60 * 60; // default is one hour
        private boolean dbOfflineMode = false;
        private Status status;

        public Builder() {
        }

        public Builder setMsisdnValidator(MsisdnValidator msisdnValidator) {
            this.msisdnValidator = msisdnValidator;
            return this;
        }

        public Builder setImsiValidator(ImsiValidator imsiValidator) {
            this.imsiValidator = imsiValidator;
            return this;
        }

        public Builder setCache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Builder setLocationProvider(LocationProvider locationProvider) {
            this.locationProvider = locationProvider;
            return this;
        }

        public Builder setProviderDao(ProviderDao providerDao) {
            this.providerDao = providerDao;
            return this;
        }

        public Builder setProfileBuilder(ProfileBuilder profileBuilder) {
            this.profileBuilder = profileBuilder;
            return this;
        }

        public Builder setLocationExpiryPeriod(long locationExpiryPeriod) {
            this.locationExpiryPeriod = locationExpiryPeriod;
            return this;
        }

        public Builder setDbOfflineMode(boolean dbOfflineMode) {
            this.dbOfflineMode = dbOfflineMode;
            return this;
        }

        public Builder setStatus(Status status) {
            this.status = status;
            return this;
        }

        public ProfileProviderImpl build() {
            assertNotNull(this.msisdnValidator, "MsisdnValidator is null");
            assertNotNull(this.imsiValidator, "ImsiValidator is null");
            assertNotNull(this.cache, "Cache is null");
            assertNotNull(this.locationProvider, "LocationProvider is null");
            assertNotNull(this.providerDao, "ProviderDao is null");
            assertNotNull(this.profileBuilder, "ProfileBuilder is null");
            assertNotNull(this.status, "Status is null");
            return new ProfileProviderImpl(this.msisdnValidator,
                    this.imsiValidator,
                    this.cache,
                    this.locationProvider,
                    this.providerDao,
                    this.profileBuilder,
                    this.locationExpiryPeriod,
                    this.dbOfflineMode,
                    this.status);
        }

        private void assertNotNull(Object o, String errorMessage) {
            if (o == null) {
                throw new IllegalStateException(errorMessage);
            }
        }

    }

    private ProfileProviderImpl(MsisdnValidator msisdnValidator, ImsiValidator imsiValidator,
                                Cache cache, LocationProvider locationProvider,
                                ProviderDao providerDao, ProfileBuilder profileBuilder, long locationExpiryPeriod,
                                boolean dbOfflineMode, Status status) {
        this.msisdnValidator = msisdnValidator;
        this.imsiValidator = imsiValidator;
        this.cache = cache;
        this.locationProvider = locationProvider;
        this.providerDao = providerDao;
        this.profileBuilder = profileBuilder;
        this.locationExpiryPeriod = locationExpiryPeriod * 1000;
        this.dbOfflineMode = dbOfflineMode;
        this.status = status;
        LOG.info("ProfileProviderImpl successfully created");
    }

    @Override
    public GetProfileResponse getProfile(String msisdn) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getProfile(%s) invoked ...", msisdn));
        }
        try {
            if (!msisdnValidator.isMsisdnValid(msisdn)) {
                throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INVALID_INPUT_3, ProfileProviderException.ERROR_DESC_INVALID_INPUT);
            }

            Profile cachedProfile = cache.get(msisdn);
            if (dbOfflineMode) {
                return handleDbOfflineMode(cachedProfile);
            }
            if (cachedProfile != null) {
                LOG.info(String.format("Profile found in cache. Checking expiration ... (%s)", msisdn));
                if (!isLocationExpired(cachedProfile)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Returning cached and not expired profile (%s)", msisdn));
                    }
                    return new GetProfileResponse(cachedProfile, GetProfileResponse.Stage.CACHED);
                } else {
                    LOG.info(String.format("Profile found in cache but expired. Checking if it is actual ... (%s)", msisdn));
                    return getProfileCheckIsActual(cachedProfile, true);
                }
            } else {
                LOG.info(String.format("Profile not found in cache. Trying DB ... (%s)", msisdn));
                return getProfileFromDatabase(msisdn);
            }
        } catch (CacheException e) {
            LOG.error(String.format("Error while getting profile from cache. Trying DB ... (%s)", msisdn), e);
            status.registerError(Status.Component.Cache);
            return getProfileFromDatabase(msisdn);
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Leaving method getProfile(%s)", msisdn));
            }
        }
    }

    @Override
    public List<GetProfileResponse> getProfileList(List<String> msisdnList) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            StringBuffer msisdns = new StringBuffer();
            for (String msisdn : msisdnList) {
                msisdns.append(msisdn).append(",");
            }
            LOG.debug(String.format("Method getProfileList(%s) invoked ...", msisdns));
        }

        List<GetProfileResponse> result = new ArrayList<GetProfileResponse>();
        for (String msisdn : msisdnList) {
            GetProfileResponse gpr = getProfile(msisdn);
            result.add(gpr);
        }
        return result;
    }

    @Override
    public GetProfileResponse getProfileByImsi(String imsi) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getProfileByImsi(%s) invoked ...", imsi));
        }
        try {
            if (!imsiValidator.isImsiValid(imsi)) {
                throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INVALID_INPUT_3, ProfileProviderException.ERROR_DESC_INVALID_INPUT);
            }

            String msisdn = null;
            try {
                msisdn = cache.getMsisdn(imsi);
            } catch (CacheException e) {
                LOG.error(String.format("Error while getting MSISDN from cache. Trying location provider ... (%s)", imsi), e);
                status.registerError(Status.Component.Cache);
            }
            if (msisdn == null) {
                LOG.info(String.format("MSISDN not found in cache. Trying location provider ... (%s)", imsi));
                try {
                    msisdn = locationProvider.getMsisdn(imsi);
                    if (msisdn == null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("MSISDN not found in location provider (%s)", imsi));
                        }
                        return new GetProfileResponse(null, GetProfileResponse.Stage.MSISDN_NOT_FOUND);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("MSISDN found in location provider. Setting to cache ... (%s, %s)", imsi, msisdn));
                    }

                    cache.setMsisdn(imsi, msisdn);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("MSISDN set to cache (%s, %s)", imsi, msisdn));
                    }

                } catch (LocationProviderException e) {
                    LOG.error(String.format("Error while getting MSISDN from location provider (%s)", imsi), e);
                    status.registerError(Status.Component.LocationProvider);
                    throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INTERNAL_ERROR_4,
                            ProfileProviderException.ERROR_DESC_INTERNAL_ERROR_4);
                } catch (CacheException e) {
                    LOG.error(String.format("Error while setting MSISDN to cache. Process continues further (%s, %s)",
                            imsi, msisdn), e);
                    status.registerError(Status.Component.Cache);
                }
            }

            GetProfileResponse response = getProfile(msisdn);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Returning profile by IMSI (%s, %s)", imsi, msisdn));
            }
            return response;
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Leaving method getProfileByImsi(%s)", imsi));
            }
        }
    }

    private GetProfileResponse getProfileCheckIsActual(Profile profile, boolean cached) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getProfileCheckIsActual(%s,%s) invoked ...", profile.getMsisdn(), cached));
        }
        try {
            Location location = locationProvider.getLocation(profile.getMsisdn());
            if (profile.isTheSameLocation(location)) {
                profile.touchLastUpdate();
                cache.set(profile);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Returning valid profile updated in cache (%s)", profile.getMsisdn()));
                }
                if (cached) {
                    return new GetProfileResponse(profile, GetProfileResponse.Stage.CACHED_LOC_CHECKED);
                } else {
                    return new GetProfileResponse(profile, GetProfileResponse.Stage.DB_LOC_CHECKED);
                }
            } else {
                LOG.info(String.format("Profile location has changed. Updating profile ... (%s)", profile.getMsisdn()));
                return updateProfile(profile.getMsisdn(), location, profile);
            }
        } catch (LocationProviderException e) {
            LOG.error(String.format("Error while checking location. Returning profile that can be outdated (%s)",
                    profile.getMsisdn()), e);
            this.status.registerError(Status.Component.LocationProvider);
            // todo if (cached) {CACHED_LOC_ERROR} else {DB_LOC_ERROR}
            return new GetProfileResponse(profile, GetProfileResponse.Stage.CACHED_LOC_ERROR);
        } catch (CacheException e) {
            LOG.error(String.format("Error while updating cache. Returning valid profile but not updated in cache (%s)",
                    profile.getMsisdn()), e);
            this.status.registerError(Status.Component.Cache);
            // todo if (cached) {CACHED_LOC_CHECKED_CACHE_ERROR} else {DB_LOC_CHECKED_CACHE_ERROR}
            return new GetProfileResponse(profile, GetProfileResponse.Stage.CACHED_LOC_CHECKED_CACHE_ERROR);
        }
    }

    private GetProfileResponse getProfileFromDatabase(String msisdn) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getProfileFromDatabase(%s) invoked ...", msisdn));
        }
        try {
            Profile profile = providerDao.getProfile(msisdn);
            if (profile != null) {
                LOG.info(String.format("Profile found in DB. Checking if it is actual ... (%s)", msisdn));
                return getProfileCheckIsActual(profile, false);
            } else {
                Location location = locationProvider.getLocation(msisdn);
                LOG.info(String.format("Profile not found in DB. Creating new profile ... (%s)", msisdn));
                return createNewProfile(msisdn, location);
            }
        } catch (ProviderDaoException e) {
            LOG.error(String.format("Error while getting profile from DB. Profile does not exist in the cache as well. Returning error (%s)",
                    msisdn), e);
            this.status.registerError(Status.Component.Database);
            throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INTERNAL_ERROR_2, ProfileProviderException.ERROR_DESC_INTERNAL_ERROR_2);
        } catch (LocationProviderException e) {
            LOG.error(String.format("Error while getting location from LD. Profile does not exist in the cache even in the database. Returning error (%s)",
                    msisdn), e);
            this.status.registerError(Status.Component.LocationProvider);
            throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INTERNAL_ERROR_2, ProfileProviderException.ERROR_DESC_INTERNAL_ERROR_2);
        }
    }

    private GetProfileResponse createNewProfile(String msisdn, Location newLocation) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method createNewProfile(%s,%s) invoked ...", msisdn, newLocation.getLocation()));
        }
        Profile newProfile = null;
        try {
            // building new profile
            newProfile = profileBuilder.buildProfile(msisdn, newLocation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("New default profile built (%s)", msisdn));
            }

            // handling persistence
            if (!newLocation.isOffnet()) {
                try {
                    providerDao.insertProfile(newProfile);
                    LOG.info(String.format("New profile inserted to DB (%s)", msisdn));
                } catch (ProviderDaoException e) {
                    if (e.getMessage().startsWith(ORA_00001_UNIQUE_CONSTRAINT_VIOLATION)) {
                        LOG.info(String.format("Unique constraint violation while inserting profile. Ignoring this rare concurrency issue (%s)",
                                msisdn), e);
                    } else {
                        throw e;
                    }
                }
            }

            // handling cache
            if (!newLocation.isOffnet()) {
                newProfile.touchLastUpdate();
                cache.set(newProfile);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Returning profile updated in cache (%s)", newProfile.getMsisdn()));
                }
                return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_INSERTED);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Returning offnet profile (%s)", newProfile.getMsisdn()));
                }
                return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_OFFNET);
            }
        } catch (ProfileBuilderException e) {
            LOG.error(String.format("Error while building default profile for location %s. Returning error (%s)",
                    newLocation.getLocation(), msisdn), e);
            throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INTERNAL_ERROR_2, ProfileProviderException.ERROR_DESC_INTERNAL_ERROR_2);
        } catch (ProviderDaoException e) {
            LOG.error(String.format("Error while persisting profile to DB. Returning new profile - not persisted, not cached (%s)",
                    msisdn), e);
            this.status.registerError(Status.Component.Database);
            return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_DB_ERROR_CACHE_NOT_UPDATED);
        } catch (CacheException e) {
            LOG.error(String.format("Error while updating cache. Returning valid new profile but not updated in the cache (%s)",
                    msisdn));
            this.status.registerError(Status.Component.Cache);
            return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_DB_UPDATED_CACHE_ERROR);
        }
    }

    private GetProfileResponse updateProfile(String msisdn, Location newLocation, Profile oldProfile) throws ProfileProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method updateProfile(%s,%s,%s) invoked ...",
                    msisdn, newLocation.getLocation(), oldProfile.getMsisdn()));
        }
        Profile newProfile = null;
        try {
            // building new profile
            newProfile = profileBuilder.buildProfile(msisdn, newLocation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("New default profile built (%s)", msisdn));
            }

            // handling persistence. assuming that oldProfile can't be offnet
            if (newLocation.isOffnet()) {
                providerDao.deleteProfile(oldProfile);
                LOG.info(String.format("Old profile removed from DB as new profile is offnet (o=%s,n=%s)", oldProfile.getMsisdn(), msisdn));
            } else {
                try {
                    providerDao.updateAttributes(newProfile);
                    LOG.info(String.format("Profile updated in DB - replacing old profile attributes (%s)", msisdn));
                } catch (ProviderDaoException e) {
                    if (e.getMessage().startsWith(ORA_00001_UNIQUE_CONSTRAINT_VIOLATION)) {
                        LOG.info(String.format("Unique constraint violation while inserting attributes. Ignoring this rare concurrency issue (%s)",
                                msisdn), e);
                    } else {
                        throw e;
                    }
                }
            }

            // handling cache
            if (!newLocation.isOffnet()) {
                newProfile.touchLastUpdate();
                cache.set(newProfile);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Returning profile updated in cache (%s)", newProfile.getMsisdn()));
                }
                return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_UPDATED);
            } else {
                cache.delete(newProfile.getMsisdn());
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Returning offnet profile, removed from cache (%s)", newProfile.getMsisdn()));
                }
                return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_OFFNET_OLD_DELETED);
            }
        } catch (ProfileBuilderException e) {
            LOG.error(String.format("Error while building default profile for location %s. Returning error (%s)",
                    newLocation.getLocation(), msisdn), e);
            throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_INTERNAL_ERROR_2, ProfileProviderException.ERROR_DESC_INTERNAL_ERROR_2);
        } catch (ProviderDaoException e) {
            LOG.error(String.format("Error while persisting profile to DB. Returning new profile - not persisted, not cached (%s)",
                    msisdn), e);
            this.status.registerError(Status.Component.Database);
            return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_DB_ERROR_CACHE_NOT_UPDATED);
        } catch (CacheException e) {
            LOG.error(String.format("Error while updating cache. Returning valid new profile but not updated in the cache (%s)",
                    msisdn));
            this.status.registerError(Status.Component.Cache);
            return new GetProfileResponse(newProfile, GetProfileResponse.Stage.NEW_DB_UPDATED_CACHE_ERROR);
        }
    }

    private boolean isLocationExpired(Profile profile) {
        return profile.getLastUpdate() + this.locationExpiryPeriod < System.currentTimeMillis();
    }

    private GetProfileResponse handleDbOfflineMode(Profile cachedProfile) throws ProfileProviderException {
        if (cachedProfile != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Returning cached profile only - DB offline mode switched on (%s)",
                        cachedProfile.getMsisdn()));
            }
            return new GetProfileResponse(cachedProfile, GetProfileResponse.Stage.CACHED_DB_OFFLINE);
        } else {
            throw new ProfileProviderException(ProfileProviderException.ERROR_CODE_MSISDN_NOT_FOUND_1, ProfileProviderException.ERROR_DESC_MSISDN_NOT_FOUND);
        }
    }

}
