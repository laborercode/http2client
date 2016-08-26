package org.laborercode.http2.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.twitter.hpack.Decoder;
import com.twitter.hpack.Encoder;

public class AbstractStream implements Stream {
    private Http2Request request;
    private Http2Response response;
    private Listener listener;
    private Encoder encoder;
    private Decoder decoder;
    private HttpHeaders headers;
    private Http2Client client;
    private FrameWriter writer;

    private int status;
    private String responseString;
    private DataBufferer dataBufferer;

    private HashMap<Integer, Frame.Push> pushFrameMap;

    AbstractStream(Http2Request request, Http2Client client) {
        this.client = client;
        this.request = request;
        this.response = new Http2Response(this);
        this.encoder = client.encoder();

        this.headers = new HttpHeaders();
        this.dataBufferer = new DataBufferer();
        this.writer = client.writer();
    }

    @Override
    public int streamId() {
        return request.streamId();
    }

    /* response part */
    @Override
    public Http2Response response() {
        if (response == null) {
            throw new IllegalStateException();
        }
        return response;
    }

    @Override
    public int status() {
        return status;
    }

    void status(int status) {
        this.status = status;
    }

    @Override
    public String responseString() {
        return responseString;
    }

    void responseString(String responseString) {
        this.responseString = responseString;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    /* end of response part */

    @Override
    public void listener(FrameListener listener) {
        this.listener = listener;
    }

    void listener(Listener listener) {
        this.listener = listener;
    }

    Listener listener() {
        return listener;
    }

    void decoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public Decoder decoder() {
        return decoder;
    }

    public DataBufferer dataBufferer() {
        return dataBufferer;
    }

    @Override
    public Http2Response headers(Http2Request.RequestMethod method, String path,
            HttpHeaders headers, int flags) {
        return headers(method, path, headers, flags, null);
    }

    @Override
    public Http2Response headers(Http2Request.RequestMethod method, String path,
            HttpHeaders headers, int flags, FrameListener listener) {
        if(listener != null) {
            this.listener = listener;
        }

        Http2HeaderBuilder builder = new Http2HeaderBuilder();
        builder.method(method.name()).path(path);

        if (headers != null) {
            for (Entry<String, Set<String>> entry : headers.headerMap().entrySet()) {
                for (String value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }

        ByteArrayOutputStream headerStream = new ByteArrayOutputStream(512);
        try {
            for (Map.Entry<byte[], byte[]> header : builder.build().entrySet()) {
                encoder.encodeHeader(headerStream, header.getKey(), header.getValue(), false);
            }
        } catch (IOException e) {

        }
        byte[] payload = headerStream.toByteArray();
        Frame.Headers frame = new Frame.Headers(streamId(), flags, payload.length);
        frame.payload(payload);
        writer.write(frame);

        return response;
    }

    @Override
    public void data(byte[] data, int flags) {
        Settings settings = client.serverSettings();
        if(settings == null) {
            return;
        }

        int frameSize = settings.getSetting(Settings.SETTINGS_MAX_FRAME_SIZE);
        int frameCount = data.length / frameSize;
        byte[] payload = null;
        for(int i = 0 ; i < frameCount ; i++) {
            if(payload == null) {
                payload = new byte[frameSize];
            }
            System.arraycopy(data, i * frameSize, payload, 0, frameSize);
            Frame.Data frame = new Frame.Data(streamId(),
                    flags ^ Http2Constants.FLAG_END_STREAM, payload.length);
            frame.payload(payload);
            writer.write(frame);
        }

        int frameRest = data.length % frameSize;
        if(frameRest >= 0) {
            payload = new byte[frameRest];
            System.arraycopy(data, frameCount * frameSize, payload, 0, frameRest);
            Frame.Data frame = new Frame.Data(streamId(), flags,
                    payload.length);
            frame.payload(payload);
            writer.write(frame);
        }
    }

    @Override
    public void settings(Settings settings) {
        byte[] payload = settings.payload();

        Frame.Settings frame = new Frame.Settings(0, 0, payload.length);
        frame.payload(payload);
        writer.write(frame);
    }

    @Override
    public void reset(int errorCode) {

    }

    @Override
    public void continuation(HttpHeaders headers, int flags) {

    }

    @Override
    public void window(int window, boolean connection) {
        byte[] payload = new byte[4];
        ByteBuffer buf = ByteBuffer.wrap(payload);
        buf.putInt(window);

        Frame.Window frame = new Frame.Window(connection ? 0 : streamId(),
                0, payload.length);
        frame.payload(payload);
        writer.write(frame);
    }

    @Override
    public void ping(byte[] data) {
        ping(data, false);
    }

    @Override
    public void ping(byte[] data, boolean ack) {
        Frame.Ping frame = new Frame.Ping(0, 0, data.length);
        frame.payload(data);
        writer.write(frame);
    }

    @Override
    public void goaway(int lastStreamId, int errorCode) {
        // TODO Auto-generated method stub
        
    }

    public void add(Frame.Push frame) {
        pushFrameMap.put(frame.pushStreamId(), frame);
    }

    public Frame.Push pushFrame(int pushStreamId) {
        return pushFrameMap.get(pushStreamId);
    }
}
