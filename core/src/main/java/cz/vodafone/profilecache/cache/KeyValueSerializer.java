package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.model.Attribute;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KeyValueSerializer {

    public String serialize(List<Attribute> entries);

}
