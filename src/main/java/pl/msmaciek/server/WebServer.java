package pl.msmaciek.server;

import com.hypixel.hytale.logger.HytaleLogger;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.websocket.VoiceChatEndpoint;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

/**
 * Manages the embedded Jetty WebSocket server for voice chat.
 */
public class WebServer {
    private final HytaleLogger logger;
    private final VoiceChatConfig config;
    private Server server;
    private boolean sslEnabled;

    public WebServer(HytaleLogger logger, VoiceChatConfig config) {
        this.logger = logger;
        this.config = config;
    }

    /**
     * Start the web server in a daemon thread.
     */
    public void startAsync() {
        Thread serverThread = new Thread(this::start, "VoiceChat-WebServer");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Start the web server (blocking).
     */
    public void start() {
        try {
            server = new Server();

            ServerConnector connector = createConnector();
            connector.setPort(config.getWebSocketPort());
            server.addConnector(connector);

            setupHandlers();

            String protocol = sslEnabled ? "https" : "http";
            log(Level.INFO, "Starting Voice Chat WebSocket server on port " + config.getWebSocketPort() + " (" + protocol + ")");

            server.start();
            server.join();
        } catch (Exception e) {
            log(Level.SEVERE, "Failed to start WebSocket server: " + e.getMessage());
        }
    }

    /**
     * Stop the web server.
     */
    public void stop() {
        if (server == null) return;

        try {
            server.stop();
            log(Level.INFO, "WebSocket server stopped");
        } catch (Exception e) {
            log(Level.WARNING, "Error stopping WebSocket server: " + e.getMessage());
        }
    }

    private ServerConnector createConnector() {
        if (!config.isUseSSL()) {
            sslEnabled = false;
            return new ServerConnector(server);
        }

        ServerConnector sslConnector = tryCreateSSLConnector();
        if (sslConnector != null) {
            sslEnabled = true;
            return sslConnector;
        }

        // Fallback to non-SSL
        sslEnabled = false;
        return new ServerConnector(server);
    }

    private ServerConnector tryCreateSSLConnector() {
        String keystorePath = resolveKeystorePath();
        if (keystorePath == null) {
            logSSLFallback("SSL keystore not found");
            return null;
        }

        try {
            SslContextFactory.Server sslContextFactory = createSSLContextFactory(keystorePath);
            sslContextFactory.start();

            HttpConfiguration httpsConfig = createHttpsConfig();

            log(Level.INFO, "SSL enabled with keystore: " + keystorePath);

            return new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
        } catch (Exception e) {
            logSSLFallback("Invalid SSL keystore: " + e.getMessage());
            return null;
        }
    }

    private File resolveKeystoreFile() {
        String keystorePath = config.getSslKeystorePath();
        File keystoreFile = new File(keystorePath);

        if (keystoreFile.isAbsolute() && keystoreFile.exists()) {
            return keystoreFile;
        }

        File modFile = new File("mods/pl.msmaciek_webvoicechat/" + keystorePath);
        if (modFile.exists()) {
            return modFile;
        }

        if (keystoreFile.exists()) {
            return keystoreFile;
        }

        return null;
    }

    private String resolveKeystorePath() {
        File file = resolveKeystoreFile();
        if (file != null) {
            return file.getAbsolutePath();
        }

        URL resourceUrl = getClass().getClassLoader().getResource(config.getSslKeystorePath());
        if (resourceUrl != null) {
            return resourceUrl.toExternalForm();
        }

        return null;
    }

    private SslContextFactory.Server createSSLContextFactory(String keystorePath) {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(keystorePath);
        sslContextFactory.setKeyStorePassword(config.getSslKeystorePassword());
        sslContextFactory.setKeyManagerPassword(config.getSslKeystorePassword());
        sslContextFactory.setSniRequired(false);
        return sslContextFactory;
    }

    private HttpConfiguration createHttpsConfig() {
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(config.getWebSocketPort());

        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setSniHostCheck(false);
        httpsConfig.addCustomizer(secureRequestCustomizer);

        return httpsConfig;
    }

    private void setupHandlers() {
        ResourceHandler resourceHandler = createResourceHandler();
        ServletContextHandler wsHandler = createWebSocketHandler();

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(wsHandler);
        server.setHandler(handlers);
    }

    private ResourceHandler createResourceHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setRedirectWelcome(false);

        URL staticUrl = getClass().getClassLoader().getResource("static");
        if (staticUrl != null) {
            resourceHandler.setResourceBase(staticUrl.toExternalForm());
        }

        return resourceHandler;
    }

    private ServletContextHandler createWebSocketHandler() {
        ServletContextHandler wsHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsHandler.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(wsHandler, (servletContext, container) -> {
            // Increased from 65536 to support higher quality audio (8192 samples * 2 bytes = 16384 per chunk)
            container.setMaxBinaryMessageSize(131072);  // 128KB for audio packets
            container.setMaxTextMessageSize(65536);
            container.addMapping("/voice", VoiceChatEndpoint.class);
        });

        return wsHandler;
    }

    private void logSSLFallback(String reason) {
        log(Level.WARNING, reason);
        log(Level.WARNING, "Falling back to non-SSL mode. Generate a keystore with:");
        log(Level.WARNING, "keytool -genkey -keyalg RSA -alias voicechat -keystore voicechat.keystore -storepass changeit -validity 365 -keysize 2048");
    }

    private void log(Level level, String message) {
        logger.at(level).log(message);
    }
}
