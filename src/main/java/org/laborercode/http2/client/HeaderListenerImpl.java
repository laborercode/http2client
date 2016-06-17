package org.laborercode.http2.client;

import com.twitter.hpack.HeaderListener;

class HeaderListenerImpl implements HeaderListener {
    private AbstractStream stream;
    private HttpHeaders headers;

    public HeaderListenerImpl(AbstractStream stream) {
        this.stream = stream;
        this.headers = stream.headers();
    }

    @Override
    public void addHeader(byte[] name, byte[] value, boolean sensitive) {
        String headerName = new String(name);
        String headerValue = new String(value);

        headers.add(headerName, headerValue);

        if(headerName.equals(":status")) {
            stream.status(Integer.parseInt(headerValue));
        }
    }
}
