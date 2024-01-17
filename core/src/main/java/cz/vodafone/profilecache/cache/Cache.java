package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.model.Profile;

public interface Cache {

    Profile get(String msisdn) throws CacheException;
    boolean set(Profile profile) throws CacheException;
    boolean delete(String msisdn) throws CacheException;
    boolean add(Profile profile) throws CacheException;

    String getMsisdn(String imsi) throws CacheException;
    boolean setMsisdn(String imsi, String msisdn) throws CacheException;

}
