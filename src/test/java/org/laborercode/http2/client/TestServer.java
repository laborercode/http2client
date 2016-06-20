package org.laborercode.http2.client;

import java.io.File;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class TestServer extends Thread {

    @Override
    public void run() {
        int httpPort = Integer.parseInt(System.getProperty("test_http.port", "8080"));
        int httpsPort = Integer.parseInt(System.getProperty("test_https.port", "8443"));
        String requestPath = System.getProperty("test.request.url", "/dummy");

        File keyStore = MavenTestingUtils.getTestResourceFile("keystore");

        Server server = new Server();

        // plain text
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(httpsPort);
        http_config.addCustomizer(new SecureRequestCustomizer());

        HttpConnectionFactory httpFactory = new HttpConnectionFactory(http_config);
        HTTP2ServerConnectionFactory http2Factory = new HTTP2ServerConnectionFactory(http_config);
        ServerConnector plainConnector = new ServerConnector(server, httpFactory, http2Factory);
        plainConnector.setPort(httpPort);

        // secured
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keyStore.getAbsolutePath());
        //sslContextFactory.setKeyStorePassword(password);
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setUseCipherSuitesOrder(true);

        NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(httpFactory.getProtocol());

        ServerConnector securedConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, alpn.getProtocol()),
                alpn, http2Factory);
        securedConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] { plainConnector, securedConnector });

        // set handler
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(DummyServlet.class, requestPath);

        server.setHandler(handler);
        try {
            server.start();
            server.join();
        } catch (Exception e) {
        }
    }
}
