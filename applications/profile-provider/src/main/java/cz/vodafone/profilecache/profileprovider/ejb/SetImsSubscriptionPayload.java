package cz.vodafone.profilecache.profileprovider.ejb;

import java.io.Serializable;

/**
 * Object message payload for setImsSubscription operation -
 *
 * @see ComptelAdapterMDB
 */
public class SetImsSubscriptionPayload implements Serializable {

    private String subscriberId;
    private String privateUserId;
    private String userPassword;

    public SetImsSubscriptionPayload(String subscriberId, String privateUserId, String userPassword) {
        this.subscriberId = subscriberId;
        this.privateUserId = privateUserId;
        this.userPassword = userPassword;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getPrivateUserId() {
        return privateUserId;
    }

    public String getUserPassword() {
        return userPassword;
    }
}
