package cz.vodafone.profilecache.profileprovider.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.List;

@WebService(targetNamespace = "http://vodafone.cz/bmg/profile")
public interface ProfileProviderService {

    @WebMethod
    public ProfileInfo getProfile(
            @WebParam(name = "subscriber") String subscriber,
            @WebParam(name = "provideOperatorId") boolean provideOperatorId)
            throws ProfileException;

    @WebMethod
    public List<ProfileInfo> getProfileList(
            @WebParam(name = "subscriberList") List<String> subscriberList,
            @WebParam(name = "provideOperatorId") boolean provideOperatorId)
            throws ProfileException;

}
