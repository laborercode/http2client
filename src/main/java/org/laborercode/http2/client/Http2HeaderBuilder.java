package org.laborercode.http2.client;

import java.util.LinkedHashMap;
import java.util.Map;

class Http2HeaderBuilder {
    private static final byte[] PSEUDO_HEADER_METHOD = ":method".getBytes();
    private static final byte[] PSEUDO_HEADER_SCHEME = ":scheme".getBytes();
    private static final byte[] PSEUDO_HEADER_AUTHORITY = ":authority".getBytes();
    private static final byte[] PSEUDO_HEADER_PATH = ":path".getBytes();

    Map<byte[], byte[]> headers = new LinkedHashMap<byte[], byte[]>();

    Http2HeaderBuilder() {
        headers.put(PSEUDO_HEADER_METHOD, Http2Request.RequestMethod.GET.name().getBytes());
        headers.put(PSEUDO_HEADER_SCHEME, "http".getBytes());
    }

    Http2HeaderBuilder method(String method) {
        return header(PSEUDO_HEADER_METHOD, method.getBytes());
    }

    Http2HeaderBuilder scheme(String scheme) {
        return header(PSEUDO_HEADER_SCHEME, scheme.getBytes());
    }

    Http2HeaderBuilder authority(String authority) {
        return header(PSEUDO_HEADER_AUTHORITY, authority.getBytes());
    }

    Http2HeaderBuilder path(String path) {
        return header(PSEUDO_HEADER_PATH, path.getBytes());
    }

    Http2HeaderBuilder header(byte[] name, byte[] value) {
        headers.put(name, value);
        return this;
    }

    Http2HeaderBuilder header(String name, String value) {
        return header(name.getBytes(), value.getBytes());
    }

    Map<byte[], byte[]> build() {
        return headers;
    }
}
