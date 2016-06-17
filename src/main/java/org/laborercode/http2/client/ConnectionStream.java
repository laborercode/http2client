package org.laborercode.http2.client;

public class ConnectionStream extends AbstractStream {

    ConnectionStream(Http2Client client) {
        super(null, client);
    }

    @Override
    public int streamId() {
        return 0;
    }
}
