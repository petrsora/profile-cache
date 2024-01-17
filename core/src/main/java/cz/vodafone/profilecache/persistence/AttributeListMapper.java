package cz.vodafone.profilecache.persistence;

import cz.vodafone.profilecache.persistence.helper.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AttributeListMapper implements Mapper<AttributeList> {

    public AttributeList map(ResultSet rs) throws SQLException {
        AttributeList attributeList = new AttributeList();
        attributeList.setId(rs.getInt("ID"));
        attributeList.setName(rs.getString("ATTRIBUTE_NAME"));
        attributeList.setValue(rs.getString("ATTRIBUTE_VALUE"));
        attributeList.setLastUpdate(rs.getTimestamp("LAST_UPDATE"));
        attributeList.setEventId(rs.getInt("EVENT_ID")); // not used so far
        return attributeList;
    }

}
