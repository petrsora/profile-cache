package cz.vodafone.profilecache.services.tibcojmsclient;

import org.apache.log4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.IllegalStateException;
import java.util.Properties;

public class JmsQueueTemplate implements ExceptionListener {

    private static final Logger LOG = Logger.getLogger(JmsQueueTemplate.class);

    private String providerUrl;
    private String username;
    private String password;
    private String queueConnectionFactoryName;
    private String queueName;
    private boolean transacted;
    private int acknowledgeMode;

    private QueueConnection connection;
    private QueueSession session;
    private Queue queue;
    private QueueReceiver receiver;
    private MessageListener listener;
    private QueueSender sender;

    private boolean connectionStarted;

    public static class Builder {
        private String providerUrl;
        private String username;
        private String password;
        private String queueConnectionFactoryName;
        private String queueName;
        private boolean transacted = false;
        private int acknowledgeMode = -1;

        public Builder() {
        }

        public Builder setProviderUrl(String providerUrl) {
            this.providerUrl = providerUrl;
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

        public Builder setQueueConnectionFactoryName(String queueConnectionFactoryName) {
            this.queueConnectionFactoryName = queueConnectionFactoryName;
            return this;
        }

        public Builder setQueueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public Builder setTransacted(boolean transacted) {
            this.transacted = transacted;
            return this;
        }

        public Builder setAcknowledgeMode(int acknowledgeMode) {
            this.acknowledgeMode = acknowledgeMode;
            return this;
        }

        public JmsQueueTemplate build() throws JMSException, NamingException {
            if (providerUrl == null) {
                throw new IllegalStateException("Missing providerUrl");
            }
            if (username == null) {
                throw new IllegalStateException("Missing username");
            }
            if (password == null) {
                throw new IllegalStateException("Missing password");
            }
            if (queueConnectionFactoryName == null) {
                throw new IllegalStateException("Missing queueConnectionFactoryName");
            }
            if (queueName == null) {
                throw new IllegalStateException("Missing queueName");
            }
            if (acknowledgeMode != QueueSession.AUTO_ACKNOWLEDGE &&
                    acknowledgeMode != QueueSession.CLIENT_ACKNOWLEDGE &&
                    acknowledgeMode != QueueSession.DUPS_OK_ACKNOWLEDGE) {
                throw new IllegalStateException("Illegal acknowledgeMode");
            }
            return new JmsQueueTemplate(this.providerUrl, this.username, this.password,
                    this.queueConnectionFactoryName, this.queueName,
                    this.transacted, this.acknowledgeMode);
        }

    }

    private JmsQueueTemplate(String providerUrl, String username, String password,
                             String queueConnectionFactoryName, String queueName,
                             boolean transacted, int acknowledgeMode) throws JMSException, NamingException {
        this.providerUrl = providerUrl;
        this.username = username;
        this.password = password;
        this.queueConnectionFactoryName = queueConnectionFactoryName;
        this.queueName = queueName;
        this.transacted = transacted;
        this.acknowledgeMode = acknowledgeMode;
        prepare();
    }

    private void prepare() throws NamingException, JMSException {
        LOG.info("Preparing JMS objects ...");
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.tibco.tibjms.naming.TibjmsInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, providerUrl);
        props.setProperty(Context.SECURITY_PRINCIPAL, username);
        props.setProperty(Context.SECURITY_CREDENTIALS, password);

        InitialContext jndiContext = new InitialContext(props);
        QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) jndiContext.lookup(queueConnectionFactoryName);
        LOG.info("QueueConnectionFactory found");

        connection = queueConnectionFactory.createQueueConnection(username, password);
        LOG.info("QueueConnection created");

        connection.setExceptionListener(this);
        LOG.info("ExceptionListener registered");

        session = connection.createQueueSession(this.transacted, this.acknowledgeMode);
        LOG.info("QueueSession created");

        queue = (Queue) jndiContext.lookup(queueName);
        LOG.info("Queue found");

        this.connectionStarted = false;
    }

    public void receiveMessages(MessageListener listener) throws JMSException, NamingException {
        if (receiver != null) {
            try {
                receiver.close();
                LOG.info("Existing receiver closed");
            } catch (JMSException e) {
                LOG.warn("Error while closing receiver", e);
            }
            receiver = null;
        }

        receiver = session.createReceiver(queue);
        LOG.info("Receiver created");

        receiver.setMessageListener(listener);
        LOG.info("Message listener registered");

        startConnection();
        this.listener = listener;
    }

    public void sendMessage(JmsMessageProducer jmsMessageProducer) throws JMSException {
        if (sender == null) {
            sender = session.createSender(queue);
            LOG.info("Sender created");
        }
        startConnection();

        Message message = jmsMessageProducer.produce(session);
        sender.send(message);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message sent");
        }
    }

    private synchronized void startConnection() throws JMSException {
        if (!connectionStarted) {
            connection.start();
            this.connectionStarted = true;
            LOG.info("JMS connection started");
        }
    }

    public void commit() throws JMSException {
        if (session == null) {
            throw new IllegalStateException("Session is null. No commit possible");
        }
        session.commit();
    }

    public void rollback() throws JMSException {
        if (session == null) {
            throw new IllegalStateException("Session is null. No rollback possible");
        }
        session.rollback();
    }

    public void disconnect() {
        LOG.info("Closing all JMS objects ...");
        try {
            if (receiver != null) {
                receiver.close();
            }
        } catch (JMSException e) {
            LOG.warn("Error while closing receiver", e);
        }
        receiver = null;

        try {
            if (sender != null) {
                sender.close();
            }
        } catch (JMSException e) {
            LOG.warn("Error while closing sender", e);
        }
        sender = null;

        try {
            if (session != null) {
                session.close();
            }
        } catch (JMSException e) {
            LOG.warn("Error while closing session", e);
        }
        session = null;

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            LOG.warn("Error while closing connection", e);
        }
        connection = null;

        LOG.info("JMS objects closed");
    }

    @Override
    public void onException(JMSException e) {
        LOG.error("Exception received while working with JMS", e);
        disconnect();
        boolean reconnected = false;
        do {
            LOG.info("Trying to reconnect to JMS ...");
            try {
                prepare();
                if (this.listener != null) {
                    receiveMessages(this.listener);
                }
                reconnected = true;
            } catch (Exception e1) {
                LOG.error("Error while reconnecting to JMS", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    LOG.error("Error while waiting for reconnection attempt", e);
                }
            }
        } while (!reconnected);
        LOG.info("Successfully reconnected to JMS");
    }

}
