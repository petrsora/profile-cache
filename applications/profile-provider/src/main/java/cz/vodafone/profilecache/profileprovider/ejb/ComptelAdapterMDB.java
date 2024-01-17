package cz.vodafone.profilecache.profileprovider.ejb;

import cz.vodafone.profilecache.profileprovider.compteladapter.ComptelAdapter;
import cz.vodafone.profilecache.profileprovider.compteladapter.ComptelAdapterException;
import cz.vodafone.profilecache.profileprovider.compteladapter.ComptelAdapterImpl;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Message driven bean used for sending generated password to Comptel adapter (asynchronously)
 */
@MessageDriven(name = "ComptelAdapterMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/comptelAdapter"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE")})
public class ComptelAdapterMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(ComptelAdapterMDB.class);

    private ComptelAdapter comptelAdapter;

    @Resource
    MessageDrivenContext mdc;

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof ObjectMessage)) {
                LOG.error("Received message is not instance of ObjectMessage. Throwing message away");
                return;
            }

            Serializable payload = ((ObjectMessage) message).getObject();
            if (!(payload instanceof SetImsSubscriptionPayload)) {
                LOG.error("Received message payload is not instance of SetImsSubscriptionPayload. Throwing message away");
                return;
            }

            SetImsSubscriptionPayload setImsSubscriptionPayload = (SetImsSubscriptionPayload) payload;
            LOG.info(String.format("Received JMS message (subscriberId=%s, privateUserId=%s)",
                    setImsSubscriptionPayload.getSubscriberId(), setImsSubscriptionPayload.getPrivateUserId()));
            this.comptelAdapter.setImsSubscription(
                    setImsSubscriptionPayload.getSubscriberId(),
                    setImsSubscriptionPayload.getPrivateUserId(),
                    setImsSubscriptionPayload.getUserPassword());

        } catch (JMSException | ComptelAdapterException e) {
            LOG.error(String.format("Error while trying to invoke comptel adapter (%s)", e.getMessage()), e);
            mdc.setRollbackOnly();
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void postConstruct() {
        LOG.info("Method postConstruct() invoked ...");

        try {
            URL wsdlLocation = new URL(Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_WSDL_LOCATION));
            String endpointAddress = Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_ENDPOINT_ADDRESS);
            String applicationCode = Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_APPLICATION_CODE);
            String userId = Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_USER_ID);
            String username = Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_USERNAME);
            String password = Configuration.getMandatoryString(ConfigurationItems.COMPTEL_ADAPTER_PASSWORD);

            this.comptelAdapter = new ComptelAdapterImpl.Builder().
                    setWsdlLocation(wsdlLocation).
                    setEndpointAddress(endpointAddress).
                    setApplicationCode(applicationCode).
                    setUserId(userId).
                    setUsername(username).
                    setPassword(password).
                    build();
        } catch (MalformedURLException e) {
            LOG.error(String.format("Fatal error while creating ComptelAdapterMDB - WSDL URL problem (%s)", e.getMessage()), e);
            throw new EJBException(e);
        }
    }

}
