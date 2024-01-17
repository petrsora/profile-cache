package cz.vodafone.profilecache.helper;

import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class FileHelper {

    private static final Logger LOG = Logger.getLogger(FileHelper.class);

    private String fileNamePrefix;
    private String fileNamePostfix;
    private DateFormat dateFormat;
    private File directory;
    private boolean appendIndex;

    private AtomicLong index = new AtomicLong(0);

    public FileHelper(String fileNamePrefix, String fileNamePostfix, DateFormat dateFormat, File directory, boolean appendIndex) {
        if (!directory.exists()) {
            String message = String.format("Specified directory does not exist (%s)", directory.toString());
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }

        this.fileNamePrefix = fileNamePrefix;
        this.fileNamePostfix = fileNamePostfix;
        this.dateFormat = dateFormat;
        this.directory = directory;
        this.appendIndex = appendIndex;
    }

    public String saveToFile(String contentToSave) {
        StringBuilder filenameBuilder = new StringBuilder().
                append(directory.toString()).
                append(File.separator).
                append(this.fileNamePrefix).
                append(dateFormat.format(new Date(System.currentTimeMillis())));

        if (this.appendIndex) {
            filenameBuilder.append("-").
                    append(index.getAndIncrement());
        }
        if (this.fileNamePostfix != null) {
            filenameBuilder.append(this.fileNamePostfix);
        }

        String filename = filenameBuilder.toString();
        BufferedWriter bw = null;
        try {
            File file = new File(filename);
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(contentToSave);
            bw.flush();
            LOG.info(String.format("Content has been saved to %s", filename));
            return filename;
        } catch (IOException e) {
            LOG.error(String.format("Error while saving content to %s", filename), e);
            return null;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LOG.error(String.format("Error while closing file %s", filename), e);
                }
            }
        }
    }


}
