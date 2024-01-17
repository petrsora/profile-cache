package cz.vodafone.profilecache.services.configuration;

public interface ConfigurationItems {

    String INTERNAL_LOCATION_EXPIRY_PERIOD = "internal.location-expiry-period"; // in seconds
    String INTERNAL_DB_OFFLINE_MODE = "internal.db-offline-mode"; // true/false

    String MEMCACHED_SERVERS = "memcached.servers";
    String MEMCACHED_EXPIRY_PERIOD = "memcached.expiry-period"; // in minutes
    String MEMCACHED_IMSI_EXPIRY_PERIOD = "memcached.imsi-expiry-period"; // in minutes
    String MEMCACHED_TIME_TO_WAIT_FOR_RESULT = "memcached.time-to-wait-for-result"; // in milliseconds

    String LOCATION_PROVIDER_WSDL_LOCATION = "location-provider.wsdl-location";
    String LOCATION_PROVIDER_ENDPOINT_ADDRESS = "location-provider.endpoint-address";
    String LOCATION_PROVIDER_APPLICATION_CODE = "location-provider.application-code";
    String LOCATION_PROVIDER_USER_ID = "location-provider.user-id";
    String LOCATION_PROVIDER_USERNAME = "location-provider.username";
    String LOCATION_PROVIDER_PASSWORD = "location-provider.password";

    String EVENTS_PROVIDER_URL = "events.provider-url";
    String EVENTS_USERNAME = "events.username";
    String EVENTS_PASSWORD = "events.password";
    String EVENTS_QUEUE_CONNECTION_FACTORY_NAME = "events.queue-connection-factory-name";
    String EVENTS_QUEUE_NAME = "events.queue-name";
    String EVENTS_FAILED_JMS_EVENTS_DIR = "events.failed-jms-events-dir";

    String MONITORING_STATUS_PAGE_ERRORS_COUNTER_RESET_INTERVAL = "monitoring.status-page.errors-counter-reset-interval"; // in seconds
    String MONITORING_PERFORMANCE_STATS_DIRECTORY = "monitoring.performance-stats.directory";
    String MONITORING_PERFORMANCE_STATS_INTERVAL = "monitoring.performance-stats.interval"; // in minutes

    // used for init-load and bulk-upload
    String DB_DRIVER = "db.driver";
    String DB_URL = "db.url";
    String DB_USERNAME = "db.username";
    String DB_PASSWORD = "db.password";

    String DB_COMMIT_THRESHOLD = "bu.db.commit.threshold";
    String DB_POOL_LIMIT = "bu.db.pool.limit";
    String MC_POOL_LIMIT = "bu.memcache.pool.limit";
    String PURGE_MC = "bu.purge.memcache";

    String COMPTEL_ADAPTER_WSDL_LOCATION = "comptel-adapter.wsdl-location";
    String COMPTEL_ADAPTER_ENDPOINT_ADDRESS = "comptel-adapter.endpoint-address";
    String COMPTEL_ADAPTER_APPLICATION_CODE = "comptel-adapter.application-code";
    String COMPTEL_ADAPTER_USER_ID = "comptel-adapter.user-id";
    String COMPTEL_ADAPTER_USERNAME = "comptel-adapter.username";
    String COMPTEL_ADAPTER_PASSWORD = "comptel-adapter.password";

}
