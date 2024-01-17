package cz.vodafone.profilecache.profileprovider.ws;

import cz.vodafone.profilecache.model.GetProfileResponse;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.ProfileProviderException;
import cz.vodafone.profilecache.profileprovider.ProfileHelper;
import cz.vodafone.profilecache.profileprovider.ejb.PerformanceMonitoringBean;
import cz.vodafone.profilecache.profileprovider.ejb.ProfileProviderBean;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@WebService(name = "Profile", endpointInterface = "cz.vodafone.profilecache.profileprovider.ws.ProfileProviderService", targetNamespace = "http://vodafone.cz/bmg/profile")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public class ProfileProviderServiceImpl implements ProfileProviderService {

    private static final Logger LOG = Logger.getLogger(ProfileProviderServiceImpl.class);

    @EJB
    private ProfileProviderBean profileProviderSessionBean;

    @EJB
    private PerformanceMonitoringBean performanceMonitoringBean;

    @Override
    public ProfileInfo getProfile(@WebParam(name = "subscriber") String subscriber,
                                  @WebParam(name = "provideOperatorId") boolean provideOperatorId)
            throws ProfileException {
        long ts = System.currentTimeMillis();
        LOG.info(String.format("Operation getProfile(%s,%s) invoked ...", subscriber, provideOperatorId));

        try {
            GetProfileResponse getProfileResponse = profileProviderSessionBean.getProfile(subscriber);

            ProfileInfo pi = extractProfileInfo(provideOperatorId, getProfileResponse);

            ts = System.currentTimeMillis() - ts;
            LOG.info(String.format("SYS=PC OP=GET-PROFILE RES=SUCCESS RT=%d MSISDN=%s (STAGE=%s %s)",
                    ts, subscriber, getProfileResponse.getStage(), pi.toString()));

            performanceMonitoringBean.registerCorrectTx(ts);
            return pi;
        } catch (ProfileProviderException e) {
            ts = System.currentTimeMillis() - ts;
            LOG.error(String.format("SYS=PC OP=GET-PROFILE RES=ERROR RT=%d MSISDN=%s CODE=%d DESC=%s",
                    ts, subscriber, e.getErrorCode(), e.getErrorDescription()), e);
            performanceMonitoringBean.registerErrorTx(ts);
            throw new ProfileException(e.getErrorCode(), e.getErrorDescription());
        }
    }

    @Override
    public List<ProfileInfo> getProfileList(@WebParam(name = "subscriberList") List<String> subscriberList,
                                            @WebParam(name = "provideOperatorId") boolean provideOperatorId)
            throws ProfileException {
        long ts = System.currentTimeMillis();
        String subscribers = collectSubscribers(subscriberList);
        LOG.info(String.format("Operation getProfileList(%s,%s) invoked ...", subscribers, provideOperatorId));

        try {
            List<GetProfileResponse> getProfileResponses = profileProviderSessionBean.getProfileList(subscriberList);

            StringBuilder profiles = new StringBuilder();
            List<ProfileInfo> pis = new ArrayList<>();
            for (GetProfileResponse getProfileResponse : getProfileResponses) {
                ProfileInfo pi = extractProfileInfo(provideOperatorId, getProfileResponse);
                pis.add(pi);
                profiles.append(String.format(" (STAGE=%s %s)", getProfileResponse.getStage(), pi.toString()));
            }

            ts = System.currentTimeMillis() - ts;
            LOG.info(String.format("SYS=PC OP=GET-PROFILE-LIST RES=SUCCESS RT=%d MSISDN-S=%s%s", ts, subscribers, profiles.toString()));
            performanceMonitoringBean.registerCorrectTx(ts);
            return pis;
        } catch (ProfileProviderException e) {
            ts = System.currentTimeMillis() - ts;
            LOG.error(String.format("SYS=PC OP=GET-PROFILE-LIST RES=ERROR RT=%d MSISDN-S=%s CODE=%d DESC=%s",
                    ts, subscribers, e.getErrorCode(), e.getErrorDescription()), e);
            performanceMonitoringBean.registerErrorTx(ts);
            throw new ProfileException(e.getErrorCode(), e.getErrorDescription());
        }
    }

    private String collectSubscribers(List<String> subscriberList) {
        StringBuilder subscribers = new StringBuilder("<");
        for (String msisdn : subscriberList) {
            subscribers.append(msisdn).append(",");
        }
        subscribers.append(">");
        return subscribers.toString();
    }

    private ProfileInfo extractProfileInfo(boolean provideOperatorId, GetProfileResponse getProfileResponse) {
        ProfileInfo pi = new ProfileInfo();

        // msisdn
        String msisdn = getProfileResponse.getProfile().getMsisdn();
        pi.setMsisdn(msisdn);

        // customer location
        String location = ProfileHelper.getAttributeValue(Profile.ATTR_LOCATION, getProfileResponse);
        pi.setCustLocation(location);

        // operator id
        String operatorId;
        if (provideOperatorId) {
            operatorId = getProfileResponse.getProfile().getOperatorId();
            pi.setOperatorId(operatorId);
        }

        // prepaid
        Boolean prepaid = ProfileHelper.getAttributeBoolean(Profile.ATTR_IS_PREPAID, getProfileResponse);
        if (prepaid != null) {
            pi.setPrepaid(prepaid);
        }

        // charging mode
        String chargingMode = "computed"; // not used so far
        pi.setChargingMode(chargingMode);

        // child
        Boolean child = ProfileHelper.getAttributeBoolean(Profile.ATTR_IS_CHILD, getProfileResponse);
        if (child != null) {
            pi.setChild(child);
        }

        // scheduling profile
        String schedulingProfile = ProfileHelper.getAttributeValue(Profile.ATTR_SCHEDULING_PROFILE, getProfileResponse);
        pi.setSchedulingProfile(schedulingProfile != null ? schedulingProfile : "");

        // restrictions
        String restrictions = ProfileHelper.getAttributeValue(Profile.ATTR_IS_RESTRICTED, getProfileResponse);
        pi.setHasRestrictions(restrictions != null && !restrictions.equals("0"));

        // timestamp
        Calendar timestamp = Calendar.getInstance();
        pi.setTimestamp(timestamp);

        // prsms barring
        Boolean prsmsBarring = ProfileHelper.getAttributeBoolean(Profile.ATTR_HAS_PRSMS_BARRING, getProfileResponse);

        // mpenezenka barring
        Boolean mpenezenkaBarring = ProfileHelper.getAttributeBoolean(Profile.ATTR_HAS_MPENEZENKA_BARRING, getProfileResponse);

        // blacklisted
        Services blacklistedServices = new Services();
        if (prsmsBarring != null && prsmsBarring) {
            blacklistedServices.getService().add("PRSMS");
        }
        if (mpenezenkaBarring != null && mpenezenkaBarring) {
            blacklistedServices.getService().add("MPENEBAR");
        }
        pi.setBlackListed(blacklistedServices);

        // whitelisted
        pi.setWhiteListed(new Services()); // not used so far

        return pi;
    }

}
