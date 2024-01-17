package cz.vodafone.profilecache.profileprovider.compteladapter;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Handler responsible for adding custom headers to HTTP request
 */
public class HeaderSOAPHandler implements SOAPHandler<SOAPMessageContext> {


    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Map protocolHeaders = (Map) context.get("org.apache.cxf.message.Message.PROTOCOL_HEADERS");
        protocolHeaders.put("SOAPAction", Collections.singletonList("/ComptelAdapterServices/SetImsSubscription"));
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {

    }
}
