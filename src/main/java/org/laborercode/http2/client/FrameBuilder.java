package org.laborercode.http2.client;

import java.nio.ByteBuffer;

import org.laborercode.http2.client.Frame.Type;

class FrameBuilder {
    public static AbstractFrame build(byte[] header) {
        ByteBuffer buf = ByteBuffer.wrap(header);
        int lengthAndType = buf.getInt();
        int length = lengthAndType >> 8;
        Type type = Type.type(lengthAndType & 0xff);
        int flags = buf.get();
        int streamId = buf.getInt();

        switch(type) {
            case DATA:
                return new Frame.Data(streamId, flags, length);
            case HEADERS:
                return new Frame.Headers(streamId, flags, length);
            case PRIORITY:
                return new Frame.Priority(streamId, flags, length);
            case RESET:
                return new Frame.Reset(streamId, flags, length);
            case SETTINGS:
                return new Frame.Settings(streamId, flags, length);
            case PUSH:
                return new Frame.Push(streamId, flags, length);
            case PING:
                return new Frame.Ping(streamId, flags, length);
            case GOAWAY:
                return new Frame.Goaway(streamId, flags, length);
            case WINDOW:
                return new Frame.Window(streamId, flags, length);
            case CONTINUATION:
                return new Frame.Continuation(streamId, flags, length);
            default:
                return new Frame.Unknown(streamId, flags, length);
        }
    }
}
