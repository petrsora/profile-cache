package cz.vodafone.profilecache;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// todo create "test shared support" module and move it there
public class Utils {

    public static void initializeLogging() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    public static boolean runIntegrationTests() {
        return Boolean.getBoolean("runIntegrationTests");
    }

}
