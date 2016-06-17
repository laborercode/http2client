package org.laborercode.http2.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.laborercode.http2.client.Frame.Type;

abstract class AbstractFrame implements Frame {
    private int streamId;
    private int length;
    protected int flags;
    protected Type type;
    private byte[] payload;

    public AbstractFrame(int streamId, int flags, int length) {
        this.streamId = streamId;
        this.flags = flags;
        this.length = length;

        type = Type.UNKNOWN;

        payload = new byte[length];
    }

    public abstract void process(AbstractStream stream) throws IOException;

    public void payload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public int streamId() {
        return streamId;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public byte[] payload() {
        return payload;
    }

    @Override
    public boolean isAck() {
        return false;
    }

    @Override
    public boolean isEndHeaders() {
        return false;
    }

    @Override
    public boolean isEndStream() {
        return false;
    }

    @Override
    public boolean isPadded() {
        return false;
    }

    @Override
    public boolean isPriority() {
        return false;
    }

    @Override
    public String toString() {
        return "StreamId : " + streamId + ", Type : " + type.name()
                + ", Flags : " + flags + ", length : " + length;
    }

    public byte[] frame() {
        byte[] frame = new byte[Http2Constants.FRAME_HEADER_SIZE + length];
        ByteBuffer buf = ByteBuffer.wrap(frame);
        buf.putInt((length << 8) + type.value);
        buf.put((byte)flags);
        buf.putInt(streamId);
        System.arraycopy(payload, 0, frame, Http2Constants.FRAME_HEADER_SIZE, length);
        return frame;
    }
}
