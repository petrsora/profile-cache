package cz.vodafone.profilecache.maintenance.impl;

import cz.vodafone.profilecache.maintenance.Statistic;
import cz.vodafone.profilecache.maintenance.StatisticRenderer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AllStatisticRenderer implements StatisticRenderer {

    // legacy format from original profile cache
    private static final String XML_TEMPLATE_FORMAT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<performanceStatistics>\n" +
                    "    <application>ProfileCache</application>\n" +
                    "    <timestamp>%s</timestamp>\n" +
                    "    <type name=\"All\">\n" +
                    "        <param>\n" +
                    "            <paramName>TotalNumberOfRequest</paramName>\n" +
                    "            <paramValue>%d</paramValue>\n" +
                    "        </param>\n" +
                    "        <param>\n" +
                    "            <paramName>TotalNumberOfError</paramName>\n" +
                    "            <paramValue>%d</paramValue>\n" +
                    "        </param>\n" +
                    "        <param>\n" +
                    "            <paramName>AverageResponseTime</paramName>\n" +
                    "            <paramValue>%d</paramValue>\n" +
                    "        </param>\n" +
                    "        <param>\n" +
                    "            <paramName>MaximumResponseTime</paramName>\n" +
                    "            <paramValue>%d</paramValue>\n" +
                    "        </param>\n" +
                    "    </type>\n" +
                    "</performanceStatistics>";

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    @Override
    public String render(Statistic stat) {
        return String.format(XML_TEMPLATE_FORMAT,
                df.format(new Date()),
                stat.getNumberOfCorrectTx() + stat.getNumberOfErrorTx(),
                stat.getNumberOfErrorTx(),
                stat.getAverageTime(),
                stat.getMaxTime());
    }
}
