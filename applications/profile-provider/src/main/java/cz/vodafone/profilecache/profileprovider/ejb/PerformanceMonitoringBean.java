package cz.vodafone.profilecache.profileprovider.ejb;

import cz.vodafone.profilecache.helper.FileHelper;
import cz.vodafone.profilecache.maintenance.PerformanceStatisticsHandler;
import cz.vodafone.profilecache.maintenance.Statistic;
import cz.vodafone.profilecache.maintenance.StatisticRenderer;
import cz.vodafone.profilecache.maintenance.impl.AllStatisticRenderer;
import cz.vodafone.profilecache.maintenance.impl.PerformanceStatisticsHandlerImpl;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import java.io.File;
import java.text.SimpleDateFormat;

@DependsOn("InitializerBean")
@Singleton
@Startup
public class PerformanceMonitoringBean implements TimedObject {

    private static final Logger LOG = Logger.getLogger(PerformanceMonitoringBean.class);

    public static final String TIMER_INFO = "performance-monitoring-timer";

    private PerformanceStatisticsHandler performanceStatisticsHandler;
    private StatisticRenderer renderer;
    private FileHelper fileHelper;

    @Resource
    TimerService timerService;

    @PostConstruct
    public void postConstruct() {
        performanceStatisticsHandler = new PerformanceStatisticsHandlerImpl();

        renderer = new AllStatisticRenderer();

        String directory = Configuration.getMandatoryString(ConfigurationItems.MONITORING_PERFORMANCE_STATS_DIRECTORY);
        // filename: Profile-Cache-Profile-Provider-2013-10-30-10-23-59.xml
        fileHelper = new FileHelper("Profile-Cache-Profile-Provider-", ".xml",
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"), new File(directory), false);

        int interval = Configuration.getMandatoryInt(ConfigurationItems.MONITORING_PERFORMANCE_STATS_INTERVAL);
        timerService.createIntervalTimer(interval * 1000 * 60, interval * 1000 * 60, new TimerConfig(TIMER_INFO, false));
    }

    @Override
    public void ejbTimeout(Timer timer) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Method ejbTimeout() invoked ...");
        }

        Statistic stat = this.performanceStatisticsHandler.getStatisticAndReset();
        String contentToSave = renderer.render(stat);
        fileHelper.saveToFile(contentToSave);

        LOG.info("Performance monitoring timer finished");
    }

    public void registerCorrectTx(long time) {
        this.performanceStatisticsHandler.registerCorrectTx(time);
    }

    public void registerErrorTx(long time) {
        this.performanceStatisticsHandler.registerErrorTx(time);
    }


}

