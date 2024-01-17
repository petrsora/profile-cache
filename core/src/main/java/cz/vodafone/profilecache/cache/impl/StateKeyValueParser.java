package cz.vodafone.profilecache.cache.impl;

import cz.vodafone.profilecache.cache.KeyValueParser;

import java.util.HashMap;
import java.util.Map;

/**
 * more efficient than TokenizerKeyValueParser
 */
public class StateKeyValueParser implements KeyValueParser {

//    Using enum is not so efficient as using byte constants
//    private enum State {READY, KEY, VALUE}

    private static byte READY = 0;
    private static byte KEY = 1;
    private static byte VALUE = 2;

    @Override
    public Map<String, String> parse(String input) {
        Map<String, String> result = new HashMap<String, String>();

        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        byte state = KEY;
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            switch (state) {
                // READY
                case 0:
                    if (c != ',') {
                        state = KEY;
                        keyBuilder = new StringBuilder();
                        keyBuilder.append(c);
                    }
                    break;
                // KEY
                case 1:
                    if (c == '=') {
                        state = VALUE;
                        valueBuilder = new StringBuilder();
                    } else {
                        keyBuilder.append(c);
                    }
                    break;
                // VALUE
                case 2:
                    if (c == ',') {
                        state = READY;
                        result.put(keyBuilder.toString(), valueBuilder.toString());
                    } else {
                        valueBuilder.append(c);
                    }
                    break;
            }
        }
        if (state == VALUE) {
            result.put(keyBuilder.toString(), valueBuilder.toString());
        }
        return result;
    }

}
