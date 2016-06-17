package org.laborercode.http2.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Http2Response {
    private Stream stream;
    private CountDownLatch latch;

    Http2Response(Stream stream) {
        this.stream = stream;
        latch = new CountDownLatch(1);
    }

    CountDownLatch latch() {
        return latch;
    }

    public int status() {
        // TODO get timeout from Http2Client
        try {
            latch.await(50000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        return stream.status();
    }

    public String response() {
        // TODO get timeout from Http2Client
        try {
            latch.await(50000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        return stream.responseString();
    }
}
