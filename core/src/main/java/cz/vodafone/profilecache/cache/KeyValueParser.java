package cz.vodafone.profilecache.cache;

import java.util.Map;

public interface KeyValueParser {

    public Map<String, String> parse(String input);

}
