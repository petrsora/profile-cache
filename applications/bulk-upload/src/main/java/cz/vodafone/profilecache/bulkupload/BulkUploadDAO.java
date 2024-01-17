package cz.vodafone.profilecache.bulkupload;

import cz.vodafone.profilecache.model.Profile;

/**
 * Created by: xpetsora
 * Date: 25.10.13
 */
public interface BulkUploadDAO {

    public String upsertProfile(Profile profile) throws BulkUploadClientException;

    public void commit() throws BulkUploadClientException;

    public void close();


}
