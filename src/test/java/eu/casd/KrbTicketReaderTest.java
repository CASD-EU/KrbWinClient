package eu.casd;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KrbTicketReaderTest {
    @Test
    void testDefaultCredentialFromLSA() throws Exception {
        // Set required properties for native SSPI
        System.setProperty("sun.security.jgss.native", "true");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        GSSManager manager = GSSManager.getInstance();

        GSSCredential credential = manager.createCredential(
                null,
                GSSCredential.INDEFINITE_LIFETIME,
                new Oid("1.2.840.113554.1.2.2"),
                GSSCredential.INITIATE_ONLY
        );

        assertNotNull(credential, "Should obtain credential from Windows LSA");
        assertTrue(credential.getRemainingLifetime() > 0, "Credential should have positive lifetime");

        GSSName name = credential.getName();
        assertNotNull(name, "Principal name should not be null");
        System.out.println("Test passed - Current Kerberos principal: " + name);

        credential.dispose();
    }
}
