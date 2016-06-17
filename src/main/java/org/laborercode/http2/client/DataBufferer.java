package org.laborercode.http2.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class DataBufferer {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

    public DataBufferer() {
    }

    public void put(byte[] buf) throws IOException {
        baos.write(buf);
    }

    public void put(byte[] buf, int offset, int length) {
        baos.write(buf, offset, length);
    }

    public byte[] data() {
        return baos.toByteArray();
    }

    public String string() {
        return baos.toString();
    }

    public void close() throws IOException {
        baos.close();
    }
}
