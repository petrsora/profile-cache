package cz.vodafone.profilecache.builder;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;

public interface ProfileBuilder {

    public Profile buildProfile(String msisdn, Location location) throws ProfileBuilderException;

}
