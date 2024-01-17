package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.cache.impl.StateKeyValueParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class KeyValueParserTest {

    private KeyValueParser keyValueParser;

    @Before
    public void before() {
        Utils.initializeLogging();
//        keyValueParser = new TokenizerKeyValueParser();
        keyValueParser = new StateKeyValueParser();
    }

    @Test
    public void tests() throws Exception {
        try {
            keyValueParser.parse(null);
            throw new Exception();
        } catch (NullPointerException e) {
            // that's ok
        }

        Map<String, String> result = keyValueParser.parse("CUST_LOC=V2,IS_PREP=T,IS_RESTRICTED=F,IS_CHILD=,HAS_MPENEBAR=F,HAS_PRSM=T,SCH_PRF=NOLIMIT");
        Assert.assertEquals(result.size(), 7);

        Assert.assertTrue(result.get("CUST_LOC").equals("V2"));
        Assert.assertTrue(result.get("IS_PREP").equals("T"));
        Assert.assertTrue(result.get("IS_RESTRICTED").equals("F"));
        Assert.assertTrue(result.get("IS_CHILD").equals(""));
        Assert.assertTrue(result.get("HAS_MPENEBAR").equals("F"));
        Assert.assertTrue(result.get("HAS_PRSM").equals("T"));
        Assert.assertTrue(result.get("SCH_PRF").equals("NOLIMIT"));

        result = keyValueParser.parse("CUST_LOC=V2,IS_PREP=T,");
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.get("CUST_LOC").equals("V2"));
        Assert.assertTrue(result.get("IS_PREP").equals("T"));

        result = keyValueParser.parse("CUST_LOC=");
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.get("CUST_LOC").equals(""));

        result = keyValueParser.parse("");
        Assert.assertEquals(result.size(), 0);

        result = keyValueParser.parse(",");
        Assert.assertEquals(result.size(), 0);

        result = keyValueParser.parse("CUST_LOC");
        Assert.assertEquals(result.size(), 0);
    }

}
