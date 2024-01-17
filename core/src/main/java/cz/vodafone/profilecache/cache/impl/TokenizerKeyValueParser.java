package cz.vodafone.profilecache.cache.impl;

import cz.vodafone.profilecache.cache.KeyValueParser;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * alternative to StateKeyValueParser
 */
public class TokenizerKeyValueParser implements KeyValueParser {
    @Override
    public Map<String, String> parse(String input) {
        Map<String, String> result = new HashMap<String, String>();

        StringTokenizer pairs = new StringTokenizer(input, ",");
        while (pairs.hasMoreTokens()) {
            String pair = pairs.nextToken();
            StringTokenizer keyValues = new StringTokenizer(pair, "=");
            String key = keyValues.nextToken();
            if (keyValues.hasMoreTokens()) {
                result.put(key, keyValues.nextToken());
            } else {
                // empty value
                if (pair.indexOf('=') != -1) {
                    result.put(key, "");
                }
            }
        }
        return result;
    }
}
