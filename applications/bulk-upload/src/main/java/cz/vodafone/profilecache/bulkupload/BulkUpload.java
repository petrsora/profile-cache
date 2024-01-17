package cz.vodafone.profilecache.bulkupload;

import cz.vodafone.profilecache.bulkupload.impl.BUPool;
import cz.vodafone.profilecache.bulkupload.impl.BUWorker;
import cz.vodafone.profilecache.bulkupload.impl.MCPool;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import cz.vodafone.profilecache.services.configuration.Initializer;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

/**
 * Created by: xpetsora
 * Date: 24.10.2013
 */
public class BulkUpload {
    private static final Logger LOG = Logger.getLogger(BulkUpload.class);
    private static BUPool buPool;
    private static MCPool mcPool;
    //private static BulkUploadConfig buConfig;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        // input arguments chk
        if (args.length < 1) {
            System.out.println("Invalid start command");
            System.out.println("java -jar bulkupload.jar <directory> [bulkupload-config.properties]");
            System.exit(1);
        }
        String directoryName = args[0];
        // buConfig
        if (args.length > 1 && args[1] != null) {
            Initializer.readConfiguration(args[1]);
        } else {
            Initializer.readConfiguration();
        }
        //buConfig = new BulkUploadConfig(args[1]);
        LOG.info("Starting Bulk upload ...  Start Time:" + new Date(startTime) + " Working directory: " + directoryName);
        File[] files = BulkUploadHelper.filterFiles((new File(directoryName)).listFiles());
        if ((files == null) || (files.length == 0)) {
            LOG.error("Working directory contains no input file(s)!");
            return;
        }
        processFiles(files);
        LOG.info("Bulk upload finished at " + new Date(System.currentTimeMillis()) + " Total elapsed time: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    public static void processFiles(File[] files) throws InterruptedException {
        // init pools
        if (LOG.isDebugEnabled()) {
            LOG.debug("Init client pools ..");
        }
        buPool = new BUPool(Configuration.getMandatoryInt(ConfigurationItems.DB_POOL_LIMIT));
        if (Configuration.getMandatoryBoolean(ConfigurationItems.PURGE_MC)) { //.booleanValue()
            mcPool = new MCPool(Configuration.getMandatoryInt(ConfigurationItems.MC_POOL_LIMIT));
        } else {
            mcPool = null;
            LOG.info("Skipping MemCache client init");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Done.");
        }
        for (File f : files) {
            long fileStartTime = System.currentTimeMillis();
            LOG.info("Started processing file " + f.getName());
            processFile(f);
            LOG.info("Finished processing file " + f.getName() + " Elapsed time: " + (System.currentTimeMillis() - fileStartTime));
        }
        // free pools
        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing client pools ..");
        }
        buPool.close();
        if (mcPool != null) {
            mcPool.close();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Done.");
        }
    }

    public static int processFile(File f) throws InterruptedException {
        int result = 0;
        int commit_threshold = Configuration.getMandatoryInt(ConfigurationItems.DB_COMMIT_THRESHOLD);
        BufferedReader reader = null;
        Vector<Profile> jobData = new Vector<Profile>(commit_threshold);

        try {
            reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null) {
                Thread.sleep(0, 200); // just let children do their job
                line = line.trim();
                // skip commented out and blank lines
                if ((line == null) || (line.isEmpty()) || "#".equals(line.substring(0, 1))) {
                    continue;
                }
                // for each (correct) line
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Line read >>>" + line + "<<<");
                }
                //construct Profile from csv line
                Profile p = BulkUploadHelper.buildProfile(line);
                if (p == null) {
                    LOG.error("Profile not created. Skipping line.");
                    continue;
                }
                //add profile to collection
                jobData.addElement(p);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Profile " + p.getMsisdn() + " added to data chunk");
                }
                //if collection size  reach/exceed commit threshold create job/submit
                if (jobData.size() >= commit_threshold) {
                    BUWorker job = new BUWorker(buPool, mcPool, jobData);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Data chunk topped. Submitting to worker thread " + job);
                    }

                    while (buPool.submit(job) == -1) {
                        Thread.sleep(200);
                    }
                    // empty/reinit collection
                    jobData = new Vector<Profile>(commit_threshold);
                }
            }
        } catch (IOException e) {
            LOG.error(String.format("Error reading file %s ! ", f.getName()), e);
            result = -1;
        } finally {
            // TODO reimplement
            // submit job if there is unsubmitted data
            if (jobData.size() >= 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Submitting last data chunk of size " + jobData.size());
                }
                BUWorker job = new BUWorker(buPool, mcPool, jobData);
                while (buPool.submit(job) == -1) {
                    Thread.sleep(200);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Data chunk empty at the end of file.");
                }
            }
            // close file and move it
            try {
                if (reader != null) {
                    reader.close();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("File " + f.getName() + " closed.");
                    }
                }
                BulkUploadHelper.moveFile(f, result == 0 ? BulkUploadHelper.SUCCESS_FOLDER : BulkUploadHelper.FAILED_FOLDER);
                return result;
            } catch (IOException e) {
                LOG.error(String.format("Cannot close file (%s)", e.getMessage()), e);
                return -1;
            }
        }


    }

}

