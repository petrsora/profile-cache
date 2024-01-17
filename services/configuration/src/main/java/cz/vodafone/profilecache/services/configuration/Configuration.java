package cz.vodafone.profilecache.services.configuration;

public class Configuration {

    public static String getMandatoryString(String name) {
        String value = getString(name);
        if (value == null) {
            throw new IllegalArgumentException(String.format("Missing configuration item (%s)", name));
        }
        return value;
    }

    public static String getString(String name) {
        return System.getProperty(name);
    }

    public static Long getMandatoryLong(String name) {
        return Long.parseLong(getMandatoryString(name));
    }

    public static Long getLong(String name) {
        String value = getString(name);
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }

    public static Integer getMandatoryInt(String name) {
        return Integer.parseInt(getMandatoryString(name));
    }

    public static Integer getInt(String name) {
        String value = getString(name);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }

    public static Boolean getMandatoryBoolean(String name) {
        return Boolean.parseBoolean(getMandatoryString(name));
    }

    public static Boolean getBoolean(String name) {
        String value = getString(name);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }


}
