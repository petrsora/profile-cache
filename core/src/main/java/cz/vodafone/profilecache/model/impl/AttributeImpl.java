package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.model.Attribute;

import java.util.Date;

public class AttributeImpl implements Attribute {

    private String name;
    private String value;
    private Date lastUpdate;

    public AttributeImpl(String name, String value) {
        this(name, value, null);
    }

    public AttributeImpl(String name, String value, Date lastUpdate) {
        this.name = name;
        this.value = value;
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public Date getLastUpdate() {
        return this.lastUpdate;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().
                append(this.name).append("=").append(this.value);
        return sb.toString();
    }
}
