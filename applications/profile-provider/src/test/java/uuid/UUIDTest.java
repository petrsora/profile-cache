package uuid;

import org.junit.Test;

/**
 * Simple UUID generation test
 */
public class UUIDTest {

    @Test
    public void tests() throws Exception {
        System.out.println(String.format("UUID.generateUUID()=%s", UUID.generateUUID()));
        System.out.println(String.format("UUID.generateUUIDCanonical()=%s", UUID.generateUUIDCanonical()));
        System.out.println(String.format("UUID.generateSecureRandomUUID()=%s", UUID.generateSecureRandomUUID()));
    }

}
