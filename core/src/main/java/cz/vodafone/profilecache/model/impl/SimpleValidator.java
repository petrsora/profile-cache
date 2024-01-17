package cz.vodafone.profilecache.model.impl;

import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * Simple validator
 *
 * @see MsisdnValidatorImpl
 * @see ImsiValidatorImpl
 */
public class SimpleValidator {

    private static final Logger LOG = Logger.getLogger(SimpleValidator.class);

    private Pattern pattern;

    public SimpleValidator(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean isValid(String input) {
        if (input == null) {
            LOG.error("Null input. Rejecting request");
            return false;
        }
        if (input.length() == 0) {
            LOG.error("Empty input. Rejecting request");
            return false;
        }

        if (!pattern.matcher(input).matches()) {
            LOG.error(String.format("Input does not match pattern \"%s\". Rejecting request", this.pattern.toString()));
            return false;
        }
        return true;
    }

}
