package cz.vodafone.profilecache.profileprovider.ejb;

import cz.vodafone.profilecache.maintenance.Status;
import cz.vodafone.profilecache.maintenance.impl.StatusImpl;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@DependsOn("InitializerBean")
@Startup
@Singleton
public class StatusHolderBean {

    private static final Logger LOG = Logger.getLogger(StatusHolderBean.class);

    private Status status;

    @PostConstruct
    public void postConstruct() {
        LOG.info("Method postConstruct() invoked ...");

        int resetInterval = Configuration.getMandatoryInt(ConfigurationItems.MONITORING_STATUS_PAGE_ERRORS_COUNTER_RESET_INTERVAL);
        this.status = new StatusImpl(resetInterval);
    }

    public Status getStatus() {
        return this.status;
    }

}