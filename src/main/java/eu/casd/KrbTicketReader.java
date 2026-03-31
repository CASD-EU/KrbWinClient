package eu.casd;
import org.ietf.jgss.*;

import java.util.Arrays;

/**
 * Java class to read/use the existing Kerberos TGT from Windows LSA cache
 * via the JDK 11+ native SSPI bridge (sspi_bridge.dll).
 *
 * This uses the built-in Java GSS-API with native SSPI support (introduced in JDK 13
 * and backported to JDK 11.0.10+). The bridge allows Java to access the LSA-generated
 * Kerberos ticket (TGT) opaquely without requiring a password or session key.
 *
 * What it "reads":
 * - The client principal (username@REALM) stored in the LSA TGT.
 * - (Optional) A service ticket for any target SPN – the GSS token contains the
 *   encrypted Kerberos service ticket (AP-REQ). Raw ticket bytes are not exposed
 *   by the public API (SSPI keeps them internal for security). If you need raw
 *   TGT bytes, you must use JNA + LSA APIs (LsaCallAuthenticationPackage), not sspi_bridge.dll.
 *
 * Requirements:
 * - JDK 11.0.10 or newer (Oracle/OpenJDK) on Windows Server 2019.
 * - sspi_bridge.dll must exist in %JAVA_HOME%\bin (it is included by default).
 * - The process must run as the same user who already has a Kerberos TGT in LSA
 *   (i.e. after domain login / kinit / smart-card auth).
 * - No krb5.conf needed – SSPI uses Windows native config.
 *
 * Usage:
 *   java -Djava.security.auth.login.config=... KerberosTicketReader [optional-SPN]
 *
 * Example:
 *   java KerberosTicketReader "HTTP/myserver.example.com"
 */
public class KrbTicketReader {

    public static void main(String[] args) {
        try {
            // === ENABLE SSPI BRIDGE (uses LSA Kerberos ticket) ===
            System.setProperty("sun.security.jgss.native", "true");
            System.setProperty("sun.security.jgss.lib", "sspi_bridge.dll");
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

            // Optional trace (uncomment for debugging):
            // System.setProperty("SSPI_BRIDGE_TRACE", "1"); // set as env var or here

            GSSManager manager = GSSManager.getInstance();

            // === READ THE TGT PRINCIPAL FROM LSA CACHE ===
            // Default credential = current user's TGT from LSA
            GSSCredential credential = manager.createCredential(
                    null,                                 // null = use default (LSA TGT)
                    GSSCredential.INDEFINITE_LIFETIME,
                    new Oid("1.2.840.113554.1.2.2"),      // Kerberos V5 OID
                    GSSCredential.INITIATE_ONLY           // client side
            );

            GSSName clientName = credential.getName();
            System.out.println("=== Kerberos TGT read from Windows LSA ===");
            System.out.println("Principal     : " + clientName);
            System.out.println("Lifetime      : " + credential.getRemainingLifetime());
            System.out.println("Mechanisms    : " + Arrays.toString(credential.getMechs()));

            // === OPTIONAL: REQUEST A SERVICE TICKET (contains the real Kerberos ticket) ===
            if (args.length > 0) {
                String spn = args[0]; // e.g. "HTTP/myserver.example.com" or "cifs/server@REALM"

                // Create target name (NT_HOSTBASED_SERVICE is the most common for SPNs)
                GSSName targetName = manager.createName(spn, GSSName.NT_HOSTBASED_SERVICE);

                GSSContext context = manager.createContext(
                        targetName,
                        new Oid("1.2.840.113554.1.2.2"),  // Kerberos
                        credential,
                        GSSContext.DEFAULT_LIFETIME
                );

                // First call to initSecContext returns the GSS token (AP-REQ containing service ticket)
                byte[] token = context.initSecContext(new byte[0], 0, 0);

                System.out.println("\n=== Service ticket generated (using LSA TGT) ===");
                System.out.println("Target SPN    : " + targetName);
                System.out.println("Token length  : " + token.length + " bytes");
                System.out.println("Token (hex)   : " + bytesToHex(token, 64)); // first 64 bytes only

                // The token above is a GSS-wrapped Kerberos AP-REQ.
                // You can parse it with a Kerberos ASN.1 parser if you really need the raw ticket.
                context.dispose();
            }

            credential.dispose();
            System.out.println("\nDone. The LSA Kerberos ticket was successfully read/used via sspi_bridge.dll.");

        } catch (GSSException e) {
            System.err.println("GSSException – check that sspi_bridge.dll exists in %JAVA_HOME%\\bin");
            System.err.println("and that you have a valid TGT in LSA (run klist in cmd).");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String bytesToHex(byte[] bytes, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, max); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }
}