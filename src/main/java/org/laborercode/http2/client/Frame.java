package org.laborercode.http2.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.twitter.hpack.Decoder;

public interface Frame {

    public int streamId();
    public Type type();
    public int length();
    public byte[] payload();

    public boolean isAck();
    public boolean isEndHeaders();
    public boolean isEndStream();
    public boolean isPadded();
    public boolean isPriority();

    public enum Type {
        DATA((byte) 0), HEADERS((byte) 1), PRIORITY((byte) 2), RESET((byte) 3),
        SETTINGS((byte) 4), PUSH((byte) 5), PING((byte) 6), GOAWAY((byte) 7),
        WINDOW((byte)8), CONTINUATION((byte)9),
        UNKNOWN((byte)0xff);

        byte value;

        Type(byte value) {
            this.value = value;
        }

        public static Type type(int t) {
            switch(t) {
                case 0: return DATA;
                case 1: return HEADERS;
                case 2: return PRIORITY;
                case 3: return RESET;
                case 4: return SETTINGS;
                case 5: return PUSH;
                case 6: return PING;
                case 7: return GOAWAY;
                case 8: return WINDOW;
                case 9: return CONTINUATION;
                default: return UNKNOWN;
            }
        }
    }

    class Data extends AbstractFrame {

        public Data(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.DATA;
        }

        @Override
        public boolean isEndStream() {
            return (flags & Http2Constants.FLAG_END_STREAM) == Http2Constants.FLAG_END_STREAM;
        }

        @Override
        public boolean isPadded() {
            return (flags & Http2Constants.FLAG_PADDED) == Http2Constants.FLAG_PADDED;
        }

        @Override
        public void process(AbstractStream stream) throws IOException {
            byte[] payload = payload();
            int offset = 0;
            int length = payload.length;

            if(isPadded()) {
                byte padLength = payload[0];
                offset += 1;
                length = length - 1 - (int)padLength & 0xff;
            }

            DataBufferer bufferer = stream.dataBufferer();
            bufferer.put(payload, offset, length);

            // set response string before call listener
            if(isEndStream()) {
                stream.responseString(bufferer.string());
                bufferer.close();
            }

            Listener listener = stream.listener();
            if(listener != null) {
                listener.onData(stream, this);
            }

            if(payload.length > 0) {
                stream.window(payload.length, true);
            }

            if(isEndStream()) {
                Http2Response response = stream.response();
                response.latch().countDown();
            } else {
                if(payload.length > 0) {
                    stream.window(payload.length, false);
                }
            }
        }
    }

    class Headers extends AbstractFrame {

        public Headers(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.HEADERS;
        }

        @Override
        public boolean isEndHeaders() {
            return (flags & Http2Constants.FLAG_END_HEADERS) == Http2Constants.FLAG_END_HEADERS;
        }

        @Override
        public boolean isEndStream() {
            return (flags & Http2Constants.FLAG_END_STREAM) == Http2Constants.FLAG_END_STREAM;
        }

        @Override
        public boolean isPadded() {
            return (flags & Http2Constants.FLAG_PADDED) == Http2Constants.FLAG_PADDED;
        }

        @Override
        public boolean isPriority() {
            return (flags & Http2Constants.FLAG_PRIORITY) == Http2Constants.FLAG_PRIORITY;
        }

        @Override
        public void process(AbstractStream stream) throws IOException {
            HeaderListenerImpl headerListener = new HeaderListenerImpl(stream);
            Decoder decoder = stream.decoder();

            byte[] payload = payload();
            int offset = 0;
            int length = payload.length;

            if(isPadded()) {
                byte padLength = payload[0];
                offset += 1;
                length = length - 1 - (int)padLength & 0xff;
            }
            if(isPriority()) {
                ByteBuffer buf = ByteBuffer.wrap(payload, offset, length);
                int exclusiveDependOnStreamId = buf.getInt();
                byte weight = buf.get();

                // set priority 
                offset += 5;
                length -= 5;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(payload, offset, length);
            decoder.decode(bais, headerListener);
            decoder.endHeaderBlock();

            Listener listener = stream.listener();
            if(listener != null) {
                listener.onHeaders(stream, this);
            }

            if(isEndStream()) {
                Http2Response response = stream.response();
                response.latch().countDown();
            }

        }
    }

    class Priority extends AbstractFrame {

        public Priority(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.PRIORITY;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onPriority(stream, this);
            }
        }
    }

    class Reset extends AbstractFrame {

        public Reset(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.RESET;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onReset(stream, this);
            }
        }
    }

    class Settings extends AbstractFrame {

        public Settings(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.SETTINGS;
        }

        @Override
        public boolean isAck() {
            return (flags & Http2Constants.FLAG_ACK) == Http2Constants.FLAG_ACK;
        }

        @Override
        public void process(AbstractStream stream) {
            if(isAck()) {
                // do nothing yet...
            } else {
                byte[] payload = payload();

                // send pong with 8 byte payload received from server
                stream.ping(payload, true);
            }
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onSettings(stream, this);
            }
        }
    }

    class Push extends AbstractFrame {

        public Push(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.PUSH;
        }

        @Override
        public boolean isEndHeaders() {
            return (flags & Http2Constants.FLAG_END_HEADERS) == Http2Constants.FLAG_END_HEADERS;
        }

        @Override
        public boolean isPadded() {
            return (flags & Http2Constants.FLAG_PADDED) == Http2Constants.FLAG_PADDED;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onPush(stream, this);
            }
        }
    }

    class Ping extends AbstractFrame {

        public Ping(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.PING;
        }

        @Override
        public boolean isAck() {
            return (flags & Http2Constants.FLAG_ACK) == Http2Constants.FLAG_ACK;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onPing(stream, this);
            }
        }
    }

    class Goaway extends AbstractFrame {

        public Goaway(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.GOAWAY;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onGoaway(stream, this);
            }
        }
    }

    class Window extends AbstractFrame {

        public Window(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.WINDOW;
        }

        @Override
        public void process(AbstractStream stream) {
            Listener listener = stream.listener();
            if(listener != null) {
                listener.onWindow(stream, this);
            }
        }

        @Override
        public String toString() {
            ByteBuffer buf = ByteBuffer.wrap(payload());
            int window = buf.getInt();
            return super.toString() + ", window : " + window;
        }
    }

    class Continuation extends AbstractFrame {

        public Continuation(int streamId, int flags, int length) {
            super(streamId, flags, length);
            this.type = Type.CONTINUATION;
        }

        @Override
        public boolean isEndHeaders() {
            return (flags & Http2Constants.FLAG_END_HEADERS) == Http2Constants.FLAG_END_HEADERS;
        }

        @Override
        public void process(AbstractStream stream) throws IOException {
            HeaderListenerImpl headerListener = new HeaderListenerImpl(stream);
            Decoder decoder = stream.decoder();

            ByteArrayInputStream bais = new ByteArrayInputStream(payload());
            decoder.decode(bais, headerListener);
            decoder.endHeaderBlock();

            Listener listener = stream.listener();
            if(listener != null) {
                listener.onContinuation(stream, this);
            }
        }
    }

    public class Unknown extends AbstractFrame {

        public Unknown(int streamId, int flags, int length) {
            super(streamId, flags, length);
            // this.type = Type.UNKNOWN is already set
        }

        @Override
        public void process(AbstractStream stream) {
            // do nothing
        }
    }
}
