package cz.vodafone.profilecache.services.configuration;

import org.apache.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Initializer {

    private static Logger LOG = Logger.getLogger(Initializer.class);

    public static final String CONFIGURATION_FILE = "/app01/profcach/conf/profile-cache-configuration.properties";

    public static void readConfiguration() throws IOException {
        readConfiguration(CONFIGURATION_FILE);
    }

    public static void readConfiguration(String confFile) throws IOException {
        LOG.info(String.format("Processing configuration file: %s", confFile));
        Properties props = new Properties();
        props.load(new FileReader(confFile));
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            System.setProperty(key, value);
            LOG.info(String.format("Configuration item set: %s=%s", key, value));
        }
        LOG.info("Configuration successfully read to system properties");
    }

}
