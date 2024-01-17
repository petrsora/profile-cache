package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.cache.impl.BuilderKeyValueSerializer;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class KeyValueSerializerTest {

    private KeyValueSerializer keyValueSerializer;

    @Before
    public void before() {
        Utils.initializeLogging();
        keyValueSerializer = new BuilderKeyValueSerializer();
    }

    @Test
    public void tests() throws Exception {
        try {
            keyValueSerializer.serialize(null);
            throw new Exception();
        } catch (NullPointerException e) {
            // that's ok
        }

        List<Attribute> input = new ArrayList<Attribute>();
        Assert.assertEquals(keyValueSerializer.serialize(input), "");

        input.add(new AttributeImpl("IS_PREP", ""));
        Assert.assertEquals("IS_PREP=", keyValueSerializer.serialize(input));

        input.add(new AttributeImpl("CUST_LOC", "V2"));
        Assert.assertEquals("IS_PREP=,CUST_LOC=V2", keyValueSerializer.serialize(input));

        input.add(new AttributeImpl("IS_CHILD", "T"));
        Assert.assertEquals("IS_PREP=,CUST_LOC=V2,IS_CHILD=T", keyValueSerializer.serialize(input));

        input.add(new AttributeImpl("HAS_PRSM", null));
        Assert.assertEquals("IS_PREP=,CUST_LOC=V2,IS_CHILD=T,HAS_PRSM=null", keyValueSerializer.serialize(input));

    }

}
