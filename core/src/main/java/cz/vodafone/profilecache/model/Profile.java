package cz.vodafone.profilecache.model;

import cz.vodafone.profilecache.location.Location;

import java.util.List;

public interface Profile {

    String ATTR_LOCATION = "CUST_LOC";
    String ATTR_IS_PREPAID = "IS_PREP";
    String ATTR_IS_RESTRICTED = "IS_RESTRICTED";
    String ATTR_IS_CHILD = "IS_CHILD";
    String ATTR_HAS_MPENEZENKA_BARRING = "HAS_MPENEBAR";
    String ATTR_HAS_PRSMS_BARRING = "HAS_PRSM";
    String ATTR_SCHEDULING_PROFILE = "SCH_PRF";
    String ATTR_IS_VOLTE = "IS_VOLTE";

    String ATTR_VALUE_TRUE = "T";
    String ATTR_VALUE_FALSE = "F";

    Integer getId();

    void setId(Integer id);

    String getMsisdn();

    String getOperatorId();

    void setAttribute(Attribute attribute);

    Attribute getAttribute(String name);

    List<Attribute> getAttributes();

    void touchLastUpdate();

    long getLastUpdate();

    boolean isTheSameLocation(Location location);

}
