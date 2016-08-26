package org.laborercode.http2.client;

import com.twitter.hpack.HeaderListener;

public class PushHeaderListener implements HeaderListener {
    private Frame.Push pushFrame;
    private HttpHeaders headers;

    public PushHeaderListener(Frame.Push frame) {
        pushFrame = frame;
        this.headers = frame.headers();
    }

    @Override
    public void addHeader(byte[] name, byte[] value, boolean sensitive) {
        String headerName = new String(name);
        String headerValue = new String(value);

        if(headerName.equals(":authority")) {
            pushFrame.authority(headerValue);
        } else if(headerName.equals(":method")) {
            pushFrame.method(headerValue);
        } else if(headerName.equals(":path")) {
            pushFrame.path(headerValue);
        } else if(headerName.equals(":scheme")) {
            pushFrame.scheme(headerValue);
        } else {
            headers.add(headerName, headerValue);
        }
    }
}
