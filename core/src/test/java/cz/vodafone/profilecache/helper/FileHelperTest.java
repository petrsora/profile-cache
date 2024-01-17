package cz.vodafone.profilecache.helper;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;

public class FileHelperTest {

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @Test
    public void testSaveToFile() {
        String directoryName = System.getProperty("java.io.tmpdir");
        File dir = new File(directoryName);

        FileHelper fh = new FileHelper("failed-jms-event-", ".txt", new SimpleDateFormat("yyyyMMdd-HHmmssSSS"), dir, true);
        String filename = fh.saveToFile("test content");
        Assert.assertNotNull(filename);
        Assert.assertTrue(filename.contains("failed-jms-event-"));
        Assert.assertTrue(filename.endsWith("-0.txt"));
        File file = new File(filename);
        Assert.assertTrue(file.exists());
        Assert.assertEquals(12, file.length());
        Assert.assertTrue(file.delete());
    }


}
