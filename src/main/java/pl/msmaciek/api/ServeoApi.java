package pl.msmaciek.api;

import com.jcraft.jsch.*;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ServeoApi {

    private static final String EXPECTED_FINGERPRINT =
            "SHA256:07jcXlJ4SkBnyTmaVnmTpXuBiRx2+Q2adxbttO9gt0M";

    @Nullable
    public static String open(int port) {
        System.out.println("[DEBUG] JSch forwarding port " + port);

        try {
            System.out.println("[DEBUG] Establishing connection...");
            Session session = getSession();
            session.connect(10_000);
            System.out.println("[DEBUG] Connection established");

            System.out.println("[DEBUG] Requesting remote port forwarding");
            session.setPortForwardingR(80, "127.0.0.1", port);

            ChannelShell shell = (ChannelShell) session.openChannel("shell");
            shell.setPty(true);

            InputStream in = shell.getInputStream();
            shell.connect();

            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("https://") && line.contains("serveousercontent.com")) {
                    return line.substring(line.indexOf("https://"));
                }
                System.err.println("[SSH] " + line);
            }
            System.err.println("[DEBUG] Did not receive URL from serveo.");
            return null;

        } catch (Throwable t) {
            System.err.println("[DEBUG] Exception: " + t);
            for (StackTraceElement e : t.getStackTrace()) {
                System.err.println("    at " + e);
            }
            return null;
        }
    }

    private static Session getSession() throws JSchException {
        JSch jsch = new JSch();

        Session session = jsch.getSession("root", "serveo.net", 22);
        session.setConfig("StrictHostKeyChecking", "no");

        session.setHostKeyRepository(new HostKeyRepository() {
            @Override
            public int check(String host, byte[] key) {
                try {
                    String fp = new HostKey(host, key).getFingerPrint(jsch);
                    System.out.println("[DEBUG] serveo.net fingerprint: " + fp);
                    return fp.equals(EXPECTED_FINGERPRINT)
                            ? HostKeyRepository.OK
                            : HostKeyRepository.NOT_INCLUDED;
                } catch (Exception e) {
                    return HostKeyRepository.NOT_INCLUDED;
                }
            }
            @Override public void add(HostKey hostkey, UserInfo ui) {}
            @Override public void remove(String host, String type) {}
            @Override public void remove(String host, String type, byte[] key) {}
            @Override public String getKnownHostsRepositoryID() { return "in-memory"; }
            @Override public HostKey[] getHostKey() { return new HostKey[0]; }
            @Override public HostKey[] getHostKey(String host, String type) { return new HostKey[0]; }
        });
        return session;
    }
}
