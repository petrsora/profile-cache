package cz.vodafone.profilecache.builder.impl;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.builder.ProfileBuilder;
import cz.vodafone.profilecache.builder.ProfileBuilderException;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import org.apache.log4j.Logger;

import java.util.*;

public class PropertiesProfileBuilder implements ProfileBuilder {

    private static final Logger LOG = Logger.getLogger(PropertiesProfileBuilder.class);

    private Properties properties;

    private Map<String, Map<String, String>> locationProfileAttributes;

    public PropertiesProfileBuilder(Properties properties) {
        this.properties = properties;
        init();
    }

    private void init() {
        locationProfileAttributes = new HashMap<String, Map<String, String>>();

        for (String key : this.properties.stringPropertyNames()) {
            if (!key.startsWith("defaultProfile")) {
                continue;
            }
            String attributeValue = this.properties.getProperty(key);
            if (attributeValue == null) {
                LOG.warn(String.format("Missing value of %s property", key));
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(key, ".-");
            try {
                tokenizer.nextToken(); // skipping defaultProfile prefix (delimiter .)
                String location = tokenizer.nextToken();
                String attributeName = tokenizer.nextToken();

                if (locationProfileAttributes.get(location) == null) {
                    locationProfileAttributes.put(location, new HashMap<String, String>());
                }
                locationProfileAttributes.get(location).put(attributeName, attributeValue);
                LOG.info(String.format(
                        "Registered default profile attribute for %s: %s=%s", location, attributeName, attributeValue));
            } catch (NoSuchElementException e) {
                LOG.warn(String.format("Wrong property %s format", key));
            }
        }
        LOG.info("Successfully initialized builder");
    }

    @Override
    public Profile buildProfile(String msisdn, Location location) throws ProfileBuilderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Building profile for %s %s ...", msisdn, location.getLocation()));
        }
        String loc = location.getLocation(); // V2, V4, S100, E103, OFFNET
        Map<String, String> attributes = this.locationProfileAttributes.get(loc);
        if (attributes == null) {
            throw new ProfileBuilderException("Missing profile attributes for location " + loc);
        }

        ProfileImpl.Builder builder = new ProfileImpl.Builder().
                setMsisdn(msisdn).
                setOperatorId(location.getOperatorId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            Attribute attr = new AttributeImpl(entry.getKey(), entry.getValue(), new Date());
            builder.setAttribute(attr);
        }
        Profile profile = builder.build();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Successfully built profile: %s", profile.toString()));
        }
        return profile;
    }

}
