package cz.vodafone.profilecache.profileprovider.ws;

import java.io.Serializable;
import java.util.Calendar;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "profileInfo")
public class ProfileInfo implements Serializable {

    private boolean child;
    private boolean hasRestrictions;
    private boolean prepaid;
    @XmlElement(required = true)
    private String custLocation;
    @XmlElement(required = true)
    private String operatorId;
    @XmlElement(required = true)
    private String schedulingProfile;
    @XmlElement(required = true)
    private String msisdn;
    @XmlElement(required = true)
    private Calendar timestamp;
    @XmlElement(required = true)
    private Services whitelisted;
    @XmlElement(required = true)
    private Services blacklisted;
    @XmlElement(required = true)
    private String chargingMode;

    public boolean isChild() {
        return child;
    }

    public void setChild(boolean child) {
        this.child = child;
    }

    public boolean isHasRestrictions() {
        return hasRestrictions;
    }

    public void setHasRestrictions(boolean hasRestrictions) {
        this.hasRestrictions = hasRestrictions;
    }

    public boolean isPrepaid() {
        return prepaid;
    }

    public void setPrepaid(boolean prepaid) {
        this.prepaid = prepaid;
    }

    public String getCustLocation() {
        return custLocation;
    }

    public void setCustLocation(String custLocation) {
        this.custLocation = custLocation;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getSchedulingProfile() {
        return schedulingProfile;
    }

    public void setSchedulingProfile(String schedulingProfile) {
        this.schedulingProfile = schedulingProfile;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public Services getWhiteListed() {
        return whitelisted;
    }

    public void setWhiteListed(Services whitelisted) {
        this.whitelisted = whitelisted;
    }

    public Services getBlackListed() {
        return blacklisted;
    }

    public void setBlackListed(Services blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String getChargingMode() {
        return chargingMode;
    }

    public void setChargingMode(String chargingMode) {
        this.chargingMode = chargingMode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MSISDN=").append(getMsisdn());
        result.append(" OPID=").append(getOperatorId());
        result.append(" LOC=").append(getCustLocation());
        result.append(" PRE=").append(isPrepaid());
        result.append(" REST=").append(isHasRestrictions());
        result.append(" CHILD=").append(isChild());
        result.append(" SCHED=").append(getSchedulingProfile());
        result.append(" CHARG=").append(getChargingMode());
        if (getBlackListed() != null && getBlackListed().getService() != null) {
            for (String service : getBlackListed().getService()) {
                result.append(" BL=").append(service);
            }
        }
        if (getWhiteListed() != null && getWhiteListed().getService() != null) {
            for (String service : getWhiteListed().getService()) {
                result.append(" WL=").append(service);
            }
        }
        return result.toString();
    }
}
