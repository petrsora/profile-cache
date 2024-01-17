package cz.vodafone.profilecache.services.tibcojmsclient;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public interface JmsMessageProducer {

    public Message produce(Session session) throws JMSException;

}
