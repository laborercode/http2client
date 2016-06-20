package org.laborercode.http2.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.laborercode.http2.client.Frame;
import org.laborercode.http2.client.FrameListener;
import org.laborercode.http2.client.Http2Client;
import org.laborercode.http2.client.Http2Request;
import org.laborercode.http2.client.Http2Response;
import org.laborercode.http2.client.HttpHeaders;
import org.laborercode.http2.client.Settings;
import org.laborercode.http2.client.Stream;
import org.laborercode.http2.client.Http2Request.RequestMethod;

public class Http2ClientTest {
    private static String testHost;
    private static int testPort;
    private static String testRequestURI;

    int requestSize = 0;

    @BeforeClass
    public static void setup() {
        testHost = System.getProperty("test.host");
        testPort = Integer.parseInt(System.getProperty("test.port"));
        testRequestURI = System.getProperty("test.request_uri");
    }

    @Test
    public void testStartWithConnectionPreface() throws IOException {
        Http2Client client = new Http2Client();
        client.connect(new InetSocketAddress(testHost, testPort));

        Http2Request request = client.request();
        Http2Response response = request.path(testRequestURI)
                .method(RequestMethod.GET)
                .send();

        Assert.assertEquals(200, response.status());

        client.disconnect();
    }

    @Test
    public void testStartWithUpgrade() throws IOException {
        Http2Client client = new Http2Client();
        UpgradeRequest.Builder builder = new UpgradeRequest.Builder();

        client.connect(testHost, testPort, request);
    }

    @Test
    public void testWindowUpdate() throws IOException {
        Http2Client client = new Http2Client();
        client.connect(new InetSocketAddress(testHost, testPort));

        Settings settings = client.serverSettings();
        int windowSize = settings.getSetting(Settings.SETTINGS_INITIAL_WINDOW_SIZE);

        Http2Request request = client.request();
        Http2Response response = request.path(testRequestURI)
                .method(RequestMethod.GET)
                .send(new FrameListener() {
                    @Override
                    public void onData(Stream stream, Frame frame) {
                        requestSize += frame.payload().length;
                    }
                });
        Assert.assertEquals(200, response.status());

        int testCount = (windowSize / requestSize + 1) * 2;
        for(int i = 0 ; i < testCount ; i++) {
            request = client.request();
            response = request.path(testRequestURI)
                    .method(RequestMethod.GET)
                    .send();

            Assert.assertEquals(200, response.status());
        }
        client.disconnect();
    }

    @Ignore @Test
    public void testPostRequest() throws IOException {
        byte[] data = new byte[] { 't', 'e', 's', 't' };
        Http2Client client = new Http2Client();
        client.connect(new InetSocketAddress(testHost, testPort));

        Http2Request request = client.request();
        Stream stream = request.stream();
        stream.window(Integer.MAX_VALUE >> 1, true);
        Http2Response response = request.path(testRequestURI)
                .method(RequestMethod.POST)
                .headers(new HttpHeaders().add("content-length", String.valueOf(data.length)))
                .body(data)
                .send();

        try {
            Assert.assertEquals(200, response.status());
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void testSendPing() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        Http2Client client = new Http2Client();
        client.testListener(new ConnectionListener() {
            @Override
            public void onPing(Stream stream, Frame frame) {
                byte[] payload = frame.payload();

                if(frame.isAck()) {
                    latch.countDown();
                }
            }
        });
        client.connect(new InetSocketAddress(testHost, testPort));

        Http2Request request = client.request();
        Stream stream = request.stream();
        stream.ping(new byte[] { 0, 0, 0, 0, 0, 0, 0, 5 });

        try {
            boolean pass = latch.await(3000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(pass);
        } finally {
            client.disconnect();
        }
    }
}
