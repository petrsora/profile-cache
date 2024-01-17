package cz.vodafone.profilecache.services.configuration.ejb;

import cz.vodafone.profilecache.services.configuration.Initializer;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Startup
@Singleton
public class InitializerBean {

    private static Logger LOG = Logger.getLogger(InitializerBean.class);

    @PostConstruct
    public void postConstruct() {
        LOG.info("Method postConstruct() invoked ...");

        try {
            String configurationFile = System.getProperty("profile-cache.configuration-file");
            if (configurationFile != null) {
                Initializer.readConfiguration(configurationFile);
            } else {
                Initializer.readConfiguration();
            }
        } catch (Exception e) {
            LOG.fatal("Fatal error while reading configuration file", e);
            throw new EJBException(e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        LOG.info("Method preDestroy() invoked ...");
    }
}
