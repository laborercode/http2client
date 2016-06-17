package org.laborercode.http2.client;

public class Http2Request {
    private RequestMethod method;
    private String path;
    private HttpHeaders headers;
    private AbstractStream stream;
    private int streamId;
    private Http2Client client;
    private byte[] data;

    public static enum RequestMethod {
        GET, POST;
    }

    Http2Request(Http2Client client, int streamId) {
        this.stream = new AbstractStream(this, client);
        this.streamId = streamId;
        this.client = client;
    }

    int streamId() {
        return streamId;
    }

    public Stream stream() {
        return stream;
    }

    public Http2Response send() {
        return send(null);
    }

    public Http2Response send(FrameListener listener) {
        if(headers == null) {
            headers = new HttpHeaders();
        }
        if(headers.get("host") == null) {
            headers.add("host", client.getHost());
        }

        if(method == null) {
            // throw exception
        }

        int headersFlags = Http2Constants.FLAG_END_HEADERS;
        if(data == null) {
            headersFlags |= Http2Constants.FLAG_END_STREAM;
        }
        Http2Response response = stream.headers(method, path, headers, 
                headersFlags, listener);

        if(data != null) {
            stream.data(data, Http2Constants.FLAG_END_STREAM);
        }
        return response;
    }

    public Http2Request method(RequestMethod method) {
        this.method = method;
        return this;
    }

    public Http2Request path(String path) {
        this.path = path;
        return this;
    }

    public Http2Request body(byte[] data) {
        this.data = data;
        return this;
    }

    public Http2Request headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }
}
