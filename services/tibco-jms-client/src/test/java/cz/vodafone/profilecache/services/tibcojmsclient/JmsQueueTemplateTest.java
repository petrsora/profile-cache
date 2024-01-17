package cz.vodafone.profilecache.services.tibcojmsclient;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;

public class JmsQueueTemplateTest {

    private static final Logger LOG = Logger.getLogger(JmsQueueTemplateTest.class);

    private class MockMessageListener implements MessageListener {

        private JmsQueueTemplate jmsQueueTemplate;
        private int maxRollbackCount;

        private int onMessageCount = 0;
        private int rollbackCount = 0;

        private MockMessageListener(JmsQueueTemplate jmsQueueTemplate, int maxRollbackCount) {
            this.jmsQueueTemplate = jmsQueueTemplate;
            this.maxRollbackCount = maxRollbackCount;
        }

        @Override
        public void onMessage(Message message) {
            incOnMessageCount();
            try {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    LOG.info("on message: " + text);

                    if (rollbackCount < maxRollbackCount) {
                        jmsQueueTemplate.rollback();
                        LOG.info("Message rolled back: " + text);
                        rollbackCount++;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            LOG.error("Error while waiting", e);
                        }
                    } else {
                        jmsQueueTemplate.commit();
                        LOG.info("Message committed: " + text);
                    }
                } else {
                    LOG.info("on message: unknown type");
                    jmsQueueTemplate.commit();
                }
            } catch (JMSException e) {
                LOG.error("Error while processing message", e);
            }
        }

        private void incOnMessageCount() {
            onMessageCount++;
        }

        private int getOnMessageCount() {
            return onMessageCount;
        }

        private void setOnMessageCount(int onMessageCount) {
            this.onMessageCount = onMessageCount;
        }
    }

    @Before
    public void before() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @Test
    public void test() throws Exception {
        if (!Boolean.getBoolean("runIntegrationTests")) {
            return;
        }
        JmsQueueTemplate jmsQueueTemplate = new JmsQueueTemplate.Builder().
                setProviderUrl("tcp://aczfil08s.vfcz.dc-ratingen.de:7222").
//                setProviderUrl("tcp://aczfil10s-z1.vfcz.dc-ratingen.de:7222").
                setUsername("profileCache").
                setPassword("profileCache123").
                setQueueConnectionFactoryName("QueueConnectionFactory").
                setQueueName("cz.vodafone.profileCache.event.xmilkohu").
//                setQueueName("cz.vodafone.profileCache.event").
                setTransacted(true).
                setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE).
                build();

        for (int i = 0; i < 10; i++) {
            jmsQueueTemplate.sendMessage(new JmsMessageProducer() {
                @Override
                public Message produce(Session session) throws JMSException {
                    String text = "xmilkohu test " + System.currentTimeMillis();
                    LOG.info("Sending message: " + text);
                    return session.createTextMessage(text);
                }
            });
            jmsQueueTemplate.commit();
        }

        MockMessageListener mockMessageListener = new MockMessageListener(jmsQueueTemplate, 5);
        jmsQueueTemplate.receiveMessages(mockMessageListener);

        Thread.sleep(4 * 1000);
        Assert.assertEquals(15, mockMessageListener.getOnMessageCount());

        jmsQueueTemplate.disconnect();
    }
}
