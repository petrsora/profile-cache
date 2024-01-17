package cz.vodafone.profilecache.services.tibcoevents;

import cz.vodafone.common.xml.common.Event;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.FileReader;

public class JaxbTest {

    @Test
    public void test() throws Exception {
//        FileReader fr = new FileReader("services/tibco-events/src/test/resources/subscriberActivationEvent.xml");
        FileReader fr = new FileReader("src/test/resources/subscriberActivationEvent.xml");

        JAXBContext context = JAXBContext.newInstance("cz.vodafone.customersubscriber.xml.events");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Event event = (Event) unmarshaller.unmarshal(fr);
        Assert.assertEquals("cz.vodafone.customersubscriber.xml.events.SubscriberActivationEvent", event.getClass().getName());
    }


}
