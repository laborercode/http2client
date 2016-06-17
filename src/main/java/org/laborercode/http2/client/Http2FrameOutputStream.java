package org.laborercode.http2.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class Http2FrameOutputStream extends OutputStream {
    private OutputStream out;

    Http2FrameOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte type, int streamId, byte flags, byte[] payload) throws IOException {
        int length = payload.length;
        byte[] frame = new byte[Http2Constants.FRAME_HEADER_SIZE + length];

        ByteBuffer buf = ByteBuffer.wrap(frame);
        buf.putInt((length << 8) + type);
        buf.put(flags);
        buf.putInt(streamId);
        System.arraycopy(payload, 0, frame, Http2Constants.FRAME_HEADER_SIZE, length);

        out.write(frame);
    }

    public void write(AbstractFrame frame) throws IOException {
        out.write(frame.frame());
    }
}
