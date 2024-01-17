package cz.vodafone.profilecache.services.tibcojmsclient;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

public class JmsQueueSender {

    public static void main(String[] args) throws Exception {
        JmsQueueTemplate jmsQueueTemplate = new JmsQueueTemplate.Builder().
                setProviderUrl("tcp://aczfil08s.vfcz.dc-ratingen.de:7222").
                setUsername("profileCache").
                setPassword("profileCache123").
                setQueueConnectionFactoryName("QueueConnectionFactory").
                setQueueName("cz.vodafone.profileCache.event.xmilkohu").
                setTransacted(true).
                setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE).
                build();

        jmsQueueTemplate.sendMessage(new JmsMessageProducer() {
            @Override
            public Message produce(Session session) throws JMSException {
                String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ns0:subscriberActivationEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                        "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                        "        <ns1:correlationId>1350292116400</ns1:correlationId>\n" +
                        "        <ns1:eventTimeStamp>2012-10-15T11:08:36.4+02:00</ns1:eventTimeStamp>\n" +
                        "        <ns1:applicationCode>V4 Tibco Integration</ns1:applicationCode>\n" +
                        "        <ns1:effectiveDate>2012-10-15T11:08:36.4+02:00</ns1:effectiveDate>\n" +
                        "    </ns1:header>\n" +
                        "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1028008181</ns1:customerAccountNumber>\n" +
                        "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">31944878</ns1:billingAccountNumber>\n" +
                        "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + "420777350243" + "</ns1:msisdn>\n" +
                        "    <ns0:reasonCode>PORDTEDIN</ns0:reasonCode>\n" +
                        "    <ns0:subscriberType>POSTPAID</ns0:subscriberType>\n" +
                        "</ns0:subscriberActivationEvent>";
                TextMessage message = session.createTextMessage(text);
                message.setStringProperty("VCZ_SUBOPERATOR", "V2");
                return message;
            }
        });
        jmsQueueTemplate.commit();

        jmsQueueTemplate.disconnect();

    }

}
