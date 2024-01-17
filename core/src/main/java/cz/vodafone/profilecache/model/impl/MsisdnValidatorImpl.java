package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.model.MsisdnValidator;

public class MsisdnValidatorImpl implements MsisdnValidator {

    private SimpleValidator validator;

    public MsisdnValidatorImpl() {
        this.validator = new SimpleValidator("[0-9]{3,15}");
    }

    @Override
    public boolean isMsisdnValid(String msisdn) {
        return validator.isValid(msisdn);
    }

}
