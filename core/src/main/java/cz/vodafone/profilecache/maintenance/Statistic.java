package cz.vodafone.profilecache.maintenance;

public class Statistic {

    private long numberOfCorrectTx;
    private long numberOfErrorTx;
    private long averageTime;
    private long maxTime;

    public Statistic(long numberOfCorrectTx, long numberOfErrorTx, long averageTime, long maxTime) {
        this.numberOfCorrectTx = numberOfCorrectTx;
        this.numberOfErrorTx = numberOfErrorTx;
        this.averageTime = averageTime;
        this.maxTime = maxTime;
    }

    public long getNumberOfCorrectTx() {
        return numberOfCorrectTx;
    }

    public long getNumberOfErrorTx() {
        return numberOfErrorTx;
    }

    public long getAverageTime() {
        return averageTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    @Override
    public String toString() {
        return String.format("numOfCorrect=%d, numOfError=%d, avgTime=%d, maxTime=%d",
                getNumberOfCorrectTx(), getNumberOfErrorTx(), getAverageTime(), getMaxTime());
    }

}
