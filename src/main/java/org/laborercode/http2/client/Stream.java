package org.laborercode.http2.client;

public interface Stream {

    public int streamId();

    public int status();

    public String responseString();

    public Http2Response response();

    public HttpHeaders headers();

    public void listener(FrameListener listener);

    public Http2Response headers(Http2Request.RequestMethod method,
            String path, HttpHeaders headers, int flags);
    public Http2Response headers(Http2Request.RequestMethod method,
            String path, HttpHeaders headers, int flags, FrameListener listener);

    public void data(byte[] data, int flags);

    public void settings(Settings settings);

    public void ping(byte[] data);
    public void ping(byte[] data, boolean ack);

    public void reset(int errorCode);

    public void goaway(int lastStreamId, int errorCode);

    public void continuation(HttpHeaders headers, int flags);

    public void window(int window, boolean connection);

}
