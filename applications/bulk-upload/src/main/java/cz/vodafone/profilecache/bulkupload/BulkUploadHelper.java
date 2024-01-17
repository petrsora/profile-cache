package cz.vodafone.profilecache.bulkupload;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Vector;

/**
 * Created by: xpetsora
 * Date: 25.10.2013
 */
public class BulkUploadHelper {
    private static final Logger LOG = Logger.getLogger(BulkUploadHelper.class);
    // const
    public static final String SUCCESS_FOLDER = "success";
    public static final String FAILED_FOLDER  = "failed";
    // csv column positions
    private static final int MSISDN = 0;
    private static final int IS_PREPAID = 1;
    private static final int IS_CHILD = 2;
    private static final int HAS_RESTRICTIONS = 3;
    private static final int HAS_PRSMS_FORBIDDEN = 4;
    private static final int SCHEDULING_PROFILE = 5;
    private static final int CUSTOMER_LOCATION = 6;
    private static final int MPENEBAR = 7;
    private static final int IS_VOLTE = 8;

    public static Profile buildProfile(String csv) {
        if (csv == null || csv.isEmpty()) {
            LOG.error("Profile line is empty! Cannot create profile.");
            return null;
        }
        try {
            ProfileImpl.Builder builder = new ProfileImpl.Builder();
            String[] item = csv.split(" *, *");
            builder.setMsisdn(item[MSISDN]);
            builder.setOperatorId(Location.OPERATOR_ID_VFCZ);
            // attrs
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item IS_PREPAID to >>" + item[IS_PREPAID] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, item[IS_PREPAID]));
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item IS_CHILD to >>" + item[IS_CHILD] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_IS_CHILD,item[IS_CHILD]));
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item HAS_RESTRICTIONS to >>" +item[HAS_RESTRICTIONS] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_IS_RESTRICTED,item[HAS_RESTRICTIONS]));
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item HAS_PRSMS_FORBIDDEN to >>" + item[HAS_PRSMS_FORBIDDEN] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_HAS_PRSMS_BARRING,item[HAS_PRSMS_FORBIDDEN]));
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item SCHEDULING_PROFILE to >>" + item[SCHEDULING_PROFILE] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_SCHEDULING_PROFILE,item[SCHEDULING_PROFILE]));
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item CUSTOMER_LOCATION to >>" + item[CUSTOMER_LOCATION] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_LOCATION,item[CUSTOMER_LOCATION]));
            String penebar;
            if (item.length<MPENEBAR+1) {
                penebar = "";
            } else {
                penebar = item[MPENEBAR];
            }
            if (LOG.isDebugEnabled()) { LOG.debug("Setting item MPENEBAR to >>" + penebar + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_HAS_MPENEZENKA_BARRING,penebar));

            if (LOG.isDebugEnabled()) { LOG.debug("Setting item IS_VOLTE to >>" + item[IS_VOLTE] + "<<"); }
            builder.setAttribute(new AttributeImpl(Profile.ATTR_IS_VOLTE, item[IS_VOLTE]));

            return builder.build();
        } catch (Exception e) {
            LOG.error(String.format("Error when building profile (%s)", e.getMessage()), e);
            return null;
        }
    }

    public static File[] filterFiles (File[]  files) {
        Vector<File> filteredFiles = new Vector<>();
        if (LOG.isDebugEnabled()) { LOG.debug("Found " + files.length + " potential chunks");}
        for (File f:files) {
            if (f.isFile()) { filteredFiles.addElement(f); }
        }
        File[] resFiles = new File[filteredFiles.size()];
        filteredFiles.copyInto(resFiles);
        if (LOG.isDebugEnabled()) { LOG.debug("Found " + resFiles.length + " file-type chunks");}
        return resFiles;
    }

    public static void moveFile(File file, String toFolder) {
        String name = file.getName();
        String slash = "/";
        if(LOG.isDebugEnabled()) { LOG.debug("Moving file:"+ name+" to "+toFolder); }
        // determine target path
        String absolutePath = file.getAbsolutePath();
        if(LOG.isDebugEnabled()) { LOG.debug("File's absolute path: "+ absolutePath); }
        int slashPos = absolutePath.lastIndexOf(slash);
        if (slashPos==-1) {
            if(LOG.isDebugEnabled()) { LOG.debug("DOS/Win path correction"); }
            slash = "\\";
            slashPos = absolutePath.lastIndexOf(slash);
        }
        if(LOG.isDebugEnabled()) { LOG.debug("slash position: "+ slashPos); }
        String sub = absolutePath.substring(0,slashPos);
        String toFolderPath = sub+slash+toFolder;
        File toFolderFile =  new File(toFolderPath);

        // create dir (if necessary )
        if(!toFolderFile.exists()){
            if(!toFolderFile.mkdir()){
                LOG.error("Directory creation failed: "+toFolderPath);
                return;
            }
        }
        // move file
        if(!file.renameTo(new File(toFolderPath, name))){
            LOG.error("Copy " + name + " to "+toFolderPath+" folder failed!");
        }
    }

}
