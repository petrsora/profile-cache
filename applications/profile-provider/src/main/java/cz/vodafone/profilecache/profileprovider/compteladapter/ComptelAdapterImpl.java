package cz.vodafone.profilecache.profileprovider.compteladapter;

import cz.vodafone.profilecache.services.compteladapterclient.*;
import org.apache.log4j.Logger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.lang.Exception;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ComptelAdapter interface
 *
 * @see ComptelAdapter
 */
public class ComptelAdapterImpl implements ComptelAdapter {

    private static final Logger LOG = Logger.getLogger(ComptelAdapterImpl.class);

    private ComptelAdapterServices services;
    private Request.Header header;

    public static class Builder {
        private URL wsdlLocation;
        private String endpointAddress;
        private String applicationCode;
        private String userId;
        private String username;
        private String password;

        public Builder setWsdlLocation(URL wsdlLocation) {
            this.wsdlLocation = wsdlLocation;
            return this;
        }

        public Builder setEndpointAddress(String endpointAddress) {
            this.endpointAddress = endpointAddress;
            return this;
        }

        public Builder setApplicationCode(String applicationCode) {
            this.applicationCode = applicationCode;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public ComptelAdapterImpl build() {
            return new ComptelAdapterImpl(this.wsdlLocation, this.endpointAddress, this.applicationCode, this.userId,
                    this.username, this.password);
        }
    }

    private ComptelAdapterImpl(URL wsdlLocation, String endpointAddress, String applicationCode, String userId,
                               String username, String password) {

        ComptelAdapterServices11 endpointService = new ComptelAdapterServices11(wsdlLocation);
        this.services = endpointService.getComptelAdapterServicesEndpoint();

        header = new Request.Header();
        header.setApplicationCode(applicationCode);
        header.setUserId(userId);

        Map<String, Object> rc = ((BindingProvider) this.services).getRequestContext();
        List<Handler> handlerChain = ((BindingProvider) this.services).getBinding().getHandlerChain();
        if (handlerChain == null) {
            handlerChain = new ArrayList<>();
        }
        handlerChain.add(new HeaderSOAPHandler());
        ((BindingProvider) this.services).getBinding().setHandlerChain(handlerChain);

        rc.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        if (username != null) {
            rc.put(BindingProvider.USERNAME_PROPERTY, username);
        }

        if (password != null) {
            rc.put(BindingProvider.PASSWORD_PROPERTY, password);
        }

        LOG.info("ComptelAdapterImpl successfully created");
    }

    @Override
    public void setImsSubscription(String subscriberID, String privateUserId, String userPassword) throws ComptelAdapterException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method setImsSubscription(%s, XXXX) invoked ...", subscriberID));
        }

        SetImsSubscriptionRequest request = new SetImsSubscriptionRequest();
        request.setHeader(header);
        request.setSubscriberId(subscriberID);
        SetImsSubscriptionRequest.PrivateUser privateUser = new SetImsSubscriptionRequest.PrivateUser();
        privateUser.setPrivateUserId(privateUserId);
        privateUser.setUserPassword(userPassword);
        request.getPrivateUser().add(privateUser);

        long ts = System.currentTimeMillis();
        try {
            SetImsSubscriptionResponse response = this.services.setImsSubscription(request);
            ts = System.currentTimeMillis() - ts;

            ReturnStatusEnum returnStatus = response.getReturnStatus();
            switch (returnStatus) {
                case SUCCESS:
                    LOG.info(String.format("SYS=CA OP=SET-IMS-SUBSCRIPTION RES=SUCCESS RT=%d SUBSCRIBER_ID=%s PRIVATE_USER_ID=%s",
                            ts, subscriberID, privateUserId));
                    return;
                case ERROR:
                    LOG.error(String.format("SYS=CA OP=SET-IMS-SUBSCRIPTION RES=ERROR RT=%d SUBSCRIBER_ID=%s PRIVATE_USER_ID=%s DESC=%s",
                            ts, subscriberID, privateUserId, "ERROR return status"));
                    throw new ComptelAdapterException(String.format("ERROR return status got for %s, %s",
                            subscriberID, privateUserId));
                default:
                    LOG.error(String.format("SYS=CA OP=SET-IMS-SUBSCRIPTION RES=ERROR RT=%d SUBSCRIBER_ID=%s PRIVATE_USER_ID=%s DESC=%s",
                            ts, subscriberID, privateUserId, "Unknown return status"));
                    throw new ComptelAdapterException(String.format("Unknown return status %s, %s, %s",
                            returnStatus.toString(), subscriberID, privateUserId));
            }

        } catch (Exception e) {
            String message = String.format("Error while querying Comptel Adapter (%s)", e.getMessage());
            LOG.error(String.format("SYS=CA OP=SET-IMS-SUBSCRIPTION RES=ERROR RT=%d SUBSCRIBER_ID=%s PRIVATE_USER_ID=%s DESC=%s",
                    (System.currentTimeMillis() - ts), subscriberID, privateUserId, message));
            throw new ComptelAdapterException(message, e);
        }
    }

}
