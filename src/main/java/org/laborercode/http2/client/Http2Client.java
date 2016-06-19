package org.laborercode.http2.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import com.twitter.hpack.Encoder;

public class Http2Client {
    private InetSocketAddress address;
    private AtomicInteger lastStreamId;
    private FrameReader reader;
    private FrameWriter writer;
    private Socket socket;
    private Settings serverSettings;
    private Settings settings;
    boolean direct = true;
    static final byte[] preface = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();
    private Encoder encoder;

    private InputStream in;
    private OutputStream out;
    private ConnectionListener listener;

    public Http2Client() {
        this(new Settings());
    }

    public Http2Client(Settings settings) {
        lastStreamId = new AtomicInteger(-1);
        this.settings = settings;
        this.encoder = new Encoder(settings.getSetting(Settings.SETTINGS_HEADER_TABLE_SIZE));
    }

    public void direct(boolean direct) {
        this.direct = direct;
    }

    public void connect(InetSocketAddress address) throws IOException {
        this.address = address;
        connect(address.getHostName(), address.getPort());
    }

    public void connect(String host, int port) throws IOException {
        this.address = new InetSocketAddress(host, port);
        socket = new Socket(address.getHostName(), address.getPort());

        upgrade(true);
        out = socket.getOutputStream();
        in = socket.getInputStream();

        writer = new FrameWriter(this);
        writer.start();

        reader = new FrameReader(this);
        reader.start();

    }

    public Http2Request connect(String host, int port, UpgradeRequest request) throws IOException {
        this.address = new InetSocketAddress(host, port);
        socket = new Socket(address.getHostName(), address.getPort());

        String requestString = request.getRequestString(host, port, settings);
        OutputStream out = socket.getOutputStream();
        out.write(requestString.getBytes());

        upgrade(false);
        this.out = out;
        in = socket.getInputStream();

        writer = new FrameWriter(this);
        writer.start();

        reader = new FrameReader(this);

        // add request for upgrade request before run reader
        Http2Request firstRequest = request();
        reader.start();

        return firstRequest;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    Settings settings() {
        return settings;
    }

    Encoder encoder() {
        return encoder;
    }

    public String getHost() {
        return address.getHostName();
    }

    public int getPort() {
        return address.getPort();
    }

    public Http2Request request() {
        lastStreamId.getAndAdd(2);
        return request(lastStreamId.get());
    }

    public Http2Request request(int streamId) {
        Http2Request request = new Http2Request(this, streamId);
        reader.add(streamId, (AbstractStream)request.stream());

        return request;
    }

    public Settings serverSettings() {
        return serverSettings;
    }

    InputStream inputStream() {
        return in;
    }

    OutputStream outputStream() {
        return out;
    }

    FrameWriter writer() {
        return writer;
    }

    private void upgrade(boolean direct) throws IOException {
        OutputStream out = socket.getOutputStream();
        if(direct) {
            out.write(preface);

            Http2FrameOutputStream frameOutputStream = new Http2FrameOutputStream(out);
            frameOutputStream.write(Frame.Type.SETTINGS.value, 0, (byte)0,
                    settings.payload());
            frameOutputStream.close();
        } else {
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = socketReader.readLine();
            if(!line.contains("101 Switching Protocols")) {
                throw new IOException();
            } else {
                out.write(preface);
            }
            while(true) {
                line = socketReader.readLine();
                if(line.isEmpty()) break;
            }
        }

        // default server settings... it will be updated right after upgrade...
        serverSettings = new Settings();
    }

    public void testListener(ConnectionListener listener) {
        this.listener = listener;
    }

    ConnectionListener testListener() {
        return listener;
    }
}
