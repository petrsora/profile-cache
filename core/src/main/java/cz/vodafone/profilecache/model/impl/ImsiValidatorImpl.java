package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.model.ImsiValidator;

public class ImsiValidatorImpl implements ImsiValidator {

    private SimpleValidator validator;

    public ImsiValidatorImpl() {
        this.validator = new SimpleValidator("[0-9]*");
    }

    @Override
    public boolean isImsiValid(String imsi) {
        return validator.isValid(imsi);
    }

}
