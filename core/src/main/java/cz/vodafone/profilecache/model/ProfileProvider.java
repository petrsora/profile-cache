package cz.vodafone.profilecache.model;


import java.util.List;

public interface ProfileProvider {

    GetProfileResponse getProfile(String msisdn) throws ProfileProviderException;

    List<GetProfileResponse> getProfileList(List<String> msisdnList) throws ProfileProviderException;

    GetProfileResponse getProfileByImsi(String imsi) throws ProfileProviderException;

}
