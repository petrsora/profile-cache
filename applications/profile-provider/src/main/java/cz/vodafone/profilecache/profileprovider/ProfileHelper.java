package cz.vodafone.profilecache.profileprovider;

import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.GetProfileResponse;
import cz.vodafone.profilecache.model.Profile;

/**
 * Helper class containing shared methods
 */
public class ProfileHelper {

    public static String getAttributeValue(String name, GetProfileResponse getProfileResponse) {
        Attribute att = getProfileResponse.getProfile().getAttribute(name);
        if (att == null) {
            return null;
        }
        return att.getValue();
    }

    public static Boolean getAttributeBoolean(String name, GetProfileResponse getProfileResponse) {
        Attribute att = getProfileResponse.getProfile().getAttribute(name);
        if (att == null) {
            return null;
        }

        if (Profile.ATTR_VALUE_TRUE.equals(att.getValue())) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }


}
