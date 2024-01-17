package cz.vodafone.profilecache.maintenance;

public interface Status {

    public enum Component {
        Cache,
        Database,
        LocationProvider
    }

    public void registerError(Component component);

    public int getErrorCount(Component component);

}
