package cz.vodafone.profilecache.location.impl;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.LocationProvider;
import cz.vodafone.profilecache.location.LocationProviderException;
import cz.vodafone.profilecache.services.locationdispatcherclient.*;
import org.apache.log4j.Logger;

import javax.xml.ws.BindingProvider;
import java.net.URL;
import java.util.Map;

public class LocationProviderImpl implements LocationProvider {

    private static final Logger LOG = Logger.getLogger(LocationProviderImpl.class);

    private GetOperatorMembershipService service;

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

        public LocationProviderImpl build() {
            return new LocationProviderImpl(this.wsdlLocation, this.endpointAddress, this.applicationCode, this.userId,
                    this.username, this.password);
        }

    }

    private LocationProviderImpl(URL wsdlLocation, String endpointAddress, String applicationCode, String userId,
                                 String username, String password) {
        GetOperatorMembershipEndpointService endpointService = new GetOperatorMembershipEndpointService(wsdlLocation);
        this.service = endpointService.getGetOperatorMembershipServicePort();

        Map<String, Object> rc = ((BindingProvider) this.service).getRequestContext();
        rc.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        if (username != null) {
            rc.put(BindingProvider.USERNAME_PROPERTY, username);
        }

        if (password != null) {
            rc.put(BindingProvider.PASSWORD_PROPERTY, password);
        }

        this.header = new Request.Header();
        this.header.setApplicationCode(applicationCode);
        this.header.setUserId(userId);
        LOG.info("LocationProviderImpl successfully created");
    }

    @Override
    public Location getLocation(String msisdn) throws LocationProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getLocation(%s) invoked ...", msisdn));
        }
        GetOperatorMembershipRequest request = new GetOperatorMembershipRequest();
        request.setMsisdn(msisdn);
        request.setHeader(header);
        request.getRequestedInfo().add(RequestedInfo.OPERATOR);
        request.getRequestedInfo().add(RequestedInfo.SUB_OPERATOR);
        long ts = System.currentTimeMillis();
        try {
            GetOperatorMembershipResponse response = service.getOperatorMembership(request);
            ts = System.currentTimeMillis() - ts;

            ReturnStatusEnum returnStatus = response.getReturnStatus();
            switch (returnStatus) {
                case SUCCESS:
                    String operatorId = response.getOperatorId();
                    if (operatorId == null || operatorId.length() == 0) {
                        LOG.error(String.format("SYS=LD OP=GET-LOCATION RES=ERROR RT=%d MSISDN=%s DESC=%s",
                                ts, msisdn, "Missing operatorId in LD response"));
                        throw new LocationProviderException("Missing operatorId in LD response");
                    }
                    if (!Location.OPERATOR_ID_VFCZ.equals(operatorId)) {
                        LOG.info(String.format("SYS=LD OP=GET-LOCATION RES=SUCCESS RT=%d MSISDN=%s OP-ID=%s SUB-OP-ID=%s",
                                ts, msisdn, operatorId, Location.LOCATION_OFFNET));
                        return new LocationImpl(operatorId, Location.LOCATION_OFFNET);
                    }

                    String subOperatorId = response.getSubOperatorId();
                    if (subOperatorId == null || subOperatorId.length() == 0) {
                        LOG.error(String.format("SYS=LD OP=GET-LOCATION RES=ERROR RT=%d MSISDN=%s DESC=%s",
                                ts, msisdn, "Missing subOperatorId in LD response"));
                        throw new LocationProviderException("Missing subOperatorId in LD response");
                    }

                    LOG.info(String.format("SYS=LD OP=GET-LOCATION RES=SUCCESS RT=%d MSISDN=%s OP-ID=%s SUB-OP-ID=%s",
                            ts, msisdn, operatorId, subOperatorId));
                    return new LocationImpl(operatorId, subOperatorId);
                case ERROR:
                    LOG.error(String.format("SYS=LD OP=GET-LOCATION RES=ERROR RT=%d MSISDN=%s DESC=%s", ts, msisdn, "ERROR return status"));
                    throw new LocationProviderException(String.format("ERROR return status got for %s", msisdn));
                default:
                    LOG.error(String.format("SYS=LD OP=GET-LOCATION RES=ERROR RT=%d MSISDN=%s DESC=%s", ts, msisdn, "Unknown return status"));
                    throw new LocationProviderException(String.format("Unknown return status (%s)", returnStatus.toString()));
            }
        } catch (javax.xml.ws.WebServiceException e) {
            LOG.error(String.format("SYS=LD OP=GET-LOCATION RES=ERROR RT=%d MSISDN=%s DESC=%s",
                    (System.currentTimeMillis() - ts), msisdn, "Error while querying LD"));
            throw new LocationProviderException("Error while querying LD", e);
        }
    }

    @Override
    public String getMsisdn(String imsi) throws LocationProviderException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method getMsisdn(%s) invoked ...", imsi));
        }
        GetOperatorMembershipRequest request = new GetOperatorMembershipRequest();
        request.setImsi(imsi);
        request.setHeader(header);
        request.getRequestedInfo().add(RequestedInfo.MSISDN);
        long ts = System.currentTimeMillis();
        try {
            GetOperatorMembershipResponse response = service.getOperatorMembership(request);
            ts = System.currentTimeMillis() - ts;

            ReturnStatusEnum returnStatus = response.getReturnStatus();
            switch (returnStatus) {
                case SUCCESS:
                    String msisdn = response.getMsisdn();
                    if (msisdn == null || msisdn.length() == 0) {
                        LOG.error(String.format("SYS=LD OP=GET-MSISDN RES=ERROR RT=%d IMSI=%s DESC=%s",
                                ts, imsi, "Missing MSISDN in LD response"));
                        throw new LocationProviderException("Missing MSISDN in LD response");
                    }
                    LOG.info(String.format("SYS=LD OP=GET-MSISDN RES=SUCCESS RT=%d IMSI=%s MSISDN=%s",
                            ts, imsi, msisdn));
                    return msisdn;
                case ERROR:
                    if (isMessageCode("LD-E-005", response)) {
                        return null;
                    } else {
                        LOG.error(String.format("SYS=LD OP=GET-MSISDN RES=ERROR RT=%d IMSI=%s DESC=%s", ts, imsi, "ERROR return status"));
                        throw new LocationProviderException(String.format("ERROR return status got for %s", imsi));
                    }
                default:
                    LOG.error(String.format("SYS=LD OP=GET-MSISDN RES=ERROR RT=%d IMSI=%s DESC=%s", ts, imsi, "Unknown return status"));
                    throw new LocationProviderException(String.format("Unknown return status (%s)", returnStatus.toString()));
            }
        } catch (javax.xml.ws.WebServiceException e) {
            LOG.error(String.format("SYS=LD OP=GET-MSISDN RES=ERROR RT=%d IMSI=%s DESC=%s",
                    (System.currentTimeMillis() - ts), imsi, "Error while querying LD"));
            throw new LocationProviderException("Error while querying LD", e);
        }
    }

    private boolean isMessageCode(String messageCode, GetOperatorMembershipResponse response) {
        if (response.getMessages() != null &&
                response.getMessages().getMessage() != null &&
                response.getMessages().getMessage().size() > 0 &&
                response.getMessages().getMessage().get(0) != null &&
                response.getMessages().getMessage().get(0).getMessageCode() != null &&
                response.getMessages().getMessage().get(0).getMessageCode().equals(messageCode)) {
            return true;
        } else {
            return false;
        }
    }
}
