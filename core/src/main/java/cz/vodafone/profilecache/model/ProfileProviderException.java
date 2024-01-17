package cz.vodafone.profilecache.model;

public class ProfileProviderException extends Exception {

    public static final int ERROR_CODE_MSISDN_NOT_FOUND_1 = 1;
    public static final String ERROR_DESC_MSISDN_NOT_FOUND = "Profile information for given MSISDN not found";

    public static final int ERROR_CODE_INTERNAL_ERROR_2 = 2;
    public static final String ERROR_DESC_INTERNAL_ERROR_2 = "Internal error occurred while retrieving profile information for given MSISDN";

    public static final int ERROR_CODE_INVALID_INPUT_3 = 3;
    public static final String ERROR_DESC_INVALID_INPUT = "Invalid input";

    public static final int ERROR_CODE_INTERNAL_ERROR_4 = 4;
    public static final String ERROR_DESC_INTERNAL_ERROR_4 = "Internal error occurred while retrieving profile information for given IMSI";

    private int errorCode;
    private String errorDescription;

    public ProfileProviderException(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public ProfileProviderException(String message, int errorCode, String errorDescription) {
        super(message);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public ProfileProviderException(String message, Throwable cause, int errorCode, String errorDescription) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public ProfileProviderException(Throwable cause, int errorCode, String errorDescription) {
        super(cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
