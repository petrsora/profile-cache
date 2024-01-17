package cz.vodafone.profilecache.cache.impl;

import cz.vodafone.profilecache.cache.KeyValueSerializer;
import cz.vodafone.profilecache.model.Attribute;

import java.util.List;

public class BuilderKeyValueSerializer implements KeyValueSerializer {

    @Override
    public String serialize(List<Attribute> entries) {
        StringBuilder builder = new StringBuilder();
        for (Attribute attr : entries) {
            builder.append(attr.getName()).append("=").append(attr.getValue()).append(",");
        }
        if (entries.size() > 0) {
            builder.deleteCharAt(builder.length()-1);
        }
        return builder.toString();
    }
}
