package cz.vodafone.profilecache.initload;

import cz.vodafone.profilecache.persistence.helper.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MsisdnAttributeMapper implements Mapper<MsisdnAttribute> {

    public MsisdnAttribute map(ResultSet rs) throws SQLException {
        MsisdnAttribute msisdnAttribute = new MsisdnAttribute();
        msisdnAttribute.setId(rs.getInt("ID"));
        msisdnAttribute.setMsisdn(rs.getString("MSISDN"));
        msisdnAttribute.setName(rs.getString("ATTRIBUTE_NAME"));
        msisdnAttribute.setValue(rs.getString("ATTRIBUTE_VALUE"));
        msisdnAttribute.setLastUpdate(rs.getTimestamp("LAST_UPDATE"));
        msisdnAttribute.setEventId(rs.getInt("EVENT_ID")); // not used so far
        return msisdnAttribute;
    }

}
