package cz.vodafone.profilecache.model;

public class GetProfileResponse {

    public enum Stage {
        MSISDN_NOT_FOUND,
        CACHED,
        CACHED_LOC_CHECKED,
        CACHED_LOC_ERROR,
        CACHED_LOC_CHECKED_CACHE_ERROR,
        CACHED_DB_OFFLINE,
        DB_LOC_CHECKED,
        NEW_INSERTED,
        NEW_UPDATED,
        NEW_OFFNET,
        NEW_OFFNET_OLD_DELETED,
        NEW_DB_ERROR_CACHE_NOT_UPDATED,
        NEW_DB_UPDATED_CACHE_ERROR
    }

    private Profile profile;
    private Stage stage;

    public GetProfileResponse(Profile profile, Stage stage) {
        this.profile = profile;
        this.stage = stage;
    }

    public Profile getProfile() {
        return profile;
    }

    public Stage getStage() {
        return stage;
    }
}
