package cz.vodafone.profilecache.events.ejb;

import cz.vodafone.profilecache.builder.impl.PropertiesProfileBuilder;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.events.EventHandler;
import cz.vodafone.profilecache.events.EventHandlerException;
import cz.vodafone.profilecache.events.EventHandlerImpl;
import cz.vodafone.profilecache.events.EventHandlerRetryException;
import cz.vodafone.profilecache.helper.FileHelper;
import cz.vodafone.profilecache.model.MsisdnValidator;
import cz.vodafone.profilecache.model.impl.MsisdnValidatorImpl;
import cz.vodafone.profilecache.persistence.ProviderDaoImpl;
import cz.vodafone.profilecache.persistence.helper.ConnectionFactory;
import cz.vodafone.profilecache.persistence.helper.DataSourceConnectionFactory;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import cz.vodafone.profilecache.services.tibcojmsclient.JmsQueueTemplate;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jms.*;
import javax.sql.DataSource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

@DependsOn("InitializerBean")
@Startup
@Singleton
public class EventsListenerBean implements MessageListener {

    private static final Logger LOG = Logger.getLogger(EventsListenerBean.class);

    // todo Use JTA?? Transaction isolation?? min pool size?? max pool size??
    @Resource(name = "java:jboss/datasources/ProfileCacheDS")
    DataSource dataSource;

    private MemcachedCache memcachedCache;

    private JmsQueueTemplate jmsQueueTemplate;
    private EventHandler eventHandler;

    private FileHelper fileHelper;

    @PostConstruct
    public void postConstruct() {
        LOG.info("Method postConstruct() invoked ...");

        try {
            String providerUrl = Configuration.getMandatoryString(ConfigurationItems.EVENTS_PROVIDER_URL);
            String username = Configuration.getMandatoryString(ConfigurationItems.EVENTS_USERNAME);
            String password = Configuration.getMandatoryString(ConfigurationItems.EVENTS_PASSWORD);
            String queueConnectionFactoryName = Configuration.getMandatoryString(ConfigurationItems.EVENTS_QUEUE_CONNECTION_FACTORY_NAME);
            String queueName = Configuration.getMandatoryString(ConfigurationItems.EVENTS_QUEUE_NAME);

            jmsQueueTemplate = new JmsQueueTemplate.Builder().
                    setProviderUrl(providerUrl).
                    setUsername(username).
                    setPassword(password).
                    setQueueConnectionFactoryName(queueConnectionFactoryName).
                    setQueueName(queueName).
                    setTransacted(true).
                    setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE).
                    build();

            String dir = Configuration.getMandatoryString(ConfigurationItems.EVENTS_FAILED_JMS_EVENTS_DIR);
            File failedJmsEventsDir = new File(dir);
            if (!failedJmsEventsDir.exists()) {
                throw new Exception(String.format("Directory for failed JMS events does not exist (%s)", dir));
            }
            fileHelper = new FileHelper("failed-jms-event-", ".txt", new SimpleDateFormat("yyyyMMdd-HHmmssSSS"),
                    failedJmsEventsDir, true);

            MsisdnValidator msisdnValidator = new MsisdnValidatorImpl();

            ConnectionFactory connectionFactory = new DataSourceConnectionFactory(dataSource);
            ProviderDaoImpl providerDao = new ProviderDaoImpl(connectionFactory);

            PropertiesProfileBuilder builder = new PropertiesProfileBuilder(System.getProperties());

            String servers = Configuration.getMandatoryString(ConfigurationItems.MEMCACHED_SERVERS);
            int expiryPeriod = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_EXPIRY_PERIOD);
            int imsiExpiryPeriod = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_IMSI_EXPIRY_PERIOD);
            int timeToWaitForResult = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_TIME_TO_WAIT_FOR_RESULT);
            memcachedCache = new MemcachedCache.Builder().
                    setBinaryProtocol().
                    setServers(servers).
                    setExpiryPeriod(expiryPeriod * 60).
                    setImsiExpiryPeriod(imsiExpiryPeriod * 60).
                    setTimeToWaitForResult(timeToWaitForResult).
                    build();


            eventHandler = new EventHandlerImpl(msisdnValidator, providerDao, builder, memcachedCache);

            jmsQueueTemplate.receiveMessages(this);
        } catch (Exception e) {
            String errMessage = String.format("Error while initializing events listener (%s)", e.getMessage());
            LOG.error(errMessage, e);
            throw new EJBException(errMessage, e);
        }

    }

    @PreDestroy
    public void preDestroy() {
        LOG.info("Method preDestroy() invoked ...");

        if (jmsQueueTemplate != null) {
            jmsQueueTemplate.disconnect();
        }

        if (memcachedCache != null) {
            memcachedCache.shutdown();
        }

    }

    @Override
    public void onMessage(Message message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Method onMessage() invoked ...");
        }

        String textMessageContent = null;
        try {
            if (!(message instanceof TextMessage)) {
                LOG.error(String.format("Unsupported message type (%s). Ignoring message", message.getClass().getName()));
                jmsQueueTemplate.commit();
                return;
            }

            textMessageContent = ((TextMessage) message).getText();
            if (textMessageContent == null || textMessageContent.length() == 0) {
                LOG.error("Empty text message. Ignoring message");
                jmsQueueTemplate.commit();
                return;
            }

            LOG.info(String.format("Incoming JMS message content:\n%s", textMessageContent));
            String propsToLog = getPropertiesToLog(message);
            LOG.info(String.format("Incoming JMS properties: %s", propsToLog));

            String subOperator = message.getStringProperty("VCZ_SUBOPERATOR");
            if (subOperator == null || subOperator.length() == 0) {
                LOG.error("Missing VCZ_SUBOPERATOR message property. Ignoring message");
                jmsQueueTemplate.commit();
                return;
            }

            eventHandler.handle(textMessageContent, subOperator);

            jmsQueueTemplate.commit();
            LOG.info("Message successfully processed and committed");
        } catch (EventHandlerRetryException e) {
            LOG.error(String.format("Error while processing message (%s). Error is recoverable, message will be rolled back to JMS queue",
                    e.getMessage()), e);
            try {
                jmsQueueTemplate.rollback();
            } catch (JMSException e1) {
                LOG.error("JMS error while rollback-ing message back to queue", e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                LOG.error("Error while waiting for returning message back to queue", e);
            }
        } catch (JMSException e) {
            LOG.error(String.format("JMS error while processing message (%s). Ignoring message", e.getMessage()), e);
            fileHelper.saveToFile(textMessageContent);
            try {
                jmsQueueTemplate.commit();
            } catch (JMSException e1) {
                LOG.error("Error while committing message", e);
            }
        } catch (EventHandlerException e) {
            LOG.error(String.format("Error while processing message (%s). Ignoring message", e.getMessage()), e);
            fileHelper.saveToFile(textMessageContent);
            try {
                jmsQueueTemplate.commit();
            } catch (JMSException e1) {
                LOG.error("Error while committing message", e);
            }
        }
    }

    private String getPropertiesToLog(Message message) {
        try {
            Enumeration propNames = message.getPropertyNames();
            if (propNames == null) {
                return "no properties";
            }
            StringBuilder result = new StringBuilder();
            while (propNames.hasMoreElements()) {
                String name = (String) propNames.nextElement();
                String value = message.getStringProperty(name);
                result.append(name).append("=").append(value);
                if (propNames.hasMoreElements()) {
                    result.append(", ");
                }
            }
            return result.toString();
        } catch (Exception e) {
            LOG.error(String.format("Error while getting JMS properties (%s)", e.getMessage()), e);
            return "ERROR";
        }
    }

}