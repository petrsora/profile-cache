package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;

import java.util.*;

public class ProfileImpl implements Profile {

    private Integer id;
    private String msisdn;
    private String operatorId;
    private long lastUpdate;
    private Map<String, Attribute> attributes;

    public static class Builder {

        private Integer id;
        private String msisdn;
        private String operatorId;
        private long lastUpdate;
        private Map<String, Attribute> attributes;

        public Builder() {
            this.attributes = new HashMap<String, Attribute>();
            this.lastUpdate = System.currentTimeMillis();
        }

        public Integer getId() {
            return id;
        }

        public Builder setId(Integer id) {
            this.id = id;
            return this;
        }

        public String getMsisdn() {
            return msisdn;
        }

        public Builder setMsisdn(String msisdn) {
            this.msisdn = msisdn;
            return this;
        }

        public Builder setOperatorId(String operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public Builder setAttribute(Attribute attribute) {
            this.attributes.put(attribute.getName(), attribute);
            return this;
        }

        public Profile build() {
            // validation
            if (this.msisdn == null || this.msisdn.length() == 0) {
                throw new IllegalStateException("Missing MSISDN");
            }

            if (this.attributes.get(ProfileImpl.ATTR_LOCATION) == null ||
                    this.attributes.get(ProfileImpl.ATTR_LOCATION).getValue().length() == 0) {
                throw new IllegalStateException("Missing location attribute");
            }

            if (this.operatorId == null || this.operatorId.length() == 0) {
                throw new IllegalStateException("Missing OperatorId");
            }

            // building profile
            Profile profile = new ProfileImpl(this.msisdn, this.operatorId, this.lastUpdate);

            // id is optional
            if (this.id != null) {
                profile.setId(this.id);
            }

            for (Attribute entry : this.attributes.values()) {
                profile.setAttribute(entry);
            }
            return profile;
        }

    }

    private ProfileImpl(String msisdn, String operatorId, long lastUpdate) {
        this.id = null;
        this.msisdn = msisdn;
        this.operatorId = operatorId;
        this.lastUpdate = lastUpdate;
        this.attributes = new HashMap<String, Attribute>();
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getMsisdn() {
        return this.msisdn;
    }

    @Override
    public String getOperatorId() {
        return this.operatorId;
    }

    @Override
    public void setAttribute(Attribute attribute) {
        this.attributes.put(attribute.getName(), attribute);
    }

    @Override
    public Attribute getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public List<Attribute> getAttributes() {
        // copying in order to make it "immutable"
        List<Attribute> newAttributes = new ArrayList<Attribute>();
        newAttributes.addAll(this.attributes.values());
        return newAttributes;
    }

    @Override
    public void touchLastUpdate() {
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public long getLastUpdate() {
        return this.lastUpdate;
    }

    @Override
    public boolean isTheSameLocation(Location location) {
        return getLocation().equals(location.getLocation());
    }

    private String getLocation() {
        return getAttribute(Profile.ATTR_LOCATION).getValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().
                append("msisdn=").append(getMsisdn()).
                append(",operatorId=").append(getOperatorId()).
                append(",id=").append(getId()).
                append(",lastUpdate=").append(this.lastUpdate).
                append(",");
        for (Attribute entry : this.attributes.values()) {
            sb.append(entry.toString()).append(",");
        }
        return sb.toString();
    }

}
