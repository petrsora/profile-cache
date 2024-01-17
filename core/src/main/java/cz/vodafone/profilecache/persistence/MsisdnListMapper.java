package cz.vodafone.profilecache.persistence;

import cz.vodafone.profilecache.persistence.helper.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MsisdnListMapper implements Mapper<MsisdnList> {

    public MsisdnList map(ResultSet rs) throws SQLException {
        MsisdnList msisdnList = new MsisdnList();
        msisdnList.setId(rs.getInt("ID"));
        msisdnList.setMsisdn(rs.getString("MSISDN"));
        return msisdnList;
    }

}
