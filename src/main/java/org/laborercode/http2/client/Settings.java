package org.laborercode.http2.client;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Settings {
    static final int SETTINGS_HEADER_TABLE_SIZE = 0x1;
    static final int SETTINGS_ENABLE_PUSH = 0x2;
    static final int SETTINGS_MAX_CONCURRENT_STREAMS = 0x3;
    static final int SETTINGS_INITIAL_WINDOW_SIZE = 0x4;
    static final int SETTINGS_MAX_FRAME_SIZE = 0x5;
    static final int SETTINGS_MAX_HEADER_LIST_SIZE = 0x6;

    Map<Integer, Integer> parameterMap;

    public Settings() {
        parameterMap = new HashMap<Integer, Integer>();
        parameterMap.put(SETTINGS_MAX_CONCURRENT_STREAMS, 128);
    }

    public Settings(byte[] payload) {
        parameterMap = new HashMap<Integer, Integer>();
        if(payload == null) return;

        ByteBuffer buf = ByteBuffer.wrap(payload);
        while(buf.remaining() >= 6) {
            int id = buf.getShort();
            int value = buf.getInt();
            parameterMap.put(id, value);
        }
    }

    public void update(Settings settings) {
        byte[] payload = settings.payload();

        ByteBuffer buf = ByteBuffer.wrap(payload);
        while(buf.remaining() >= 6) {
            int id = buf.getShort();
            int value = buf.getInt();
            parameterMap.put(id, value);
        }
    }

    public int getSetting(int id) {
        Integer value = parameterMap.get(id);
        if (value == null) {
            value = defaults(id);
        }
        if (value == -1) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    private int defaults(int id) {
        switch (id) {
            case SETTINGS_HEADER_TABLE_SIZE:
                return 4096;
            case SETTINGS_ENABLE_PUSH:
                return 1;
            case SETTINGS_INITIAL_WINDOW_SIZE:
                return 65535;
            case SETTINGS_MAX_FRAME_SIZE:
                return 16384;
            case SETTINGS_MAX_HEADER_LIST_SIZE:
                return Integer.MAX_VALUE;
            default:
                return -1;
        }
    }

    public Settings concurrentStreams(int streams) {
        parameterMap.put(SETTINGS_MAX_CONCURRENT_STREAMS, streams);
        return this;
    }

    public Settings enablePush(boolean enable) {
        parameterMap.put(SETTINGS_ENABLE_PUSH, enable ? 1 : 0);
        return this;
    }

    public Settings frameSize(int size) {
        parameterMap.put(SETTINGS_MAX_FRAME_SIZE, size);
        return this;
    }

    byte[] payload() {
        int length = parameterMap.size() * 6;
        byte[] payload = new byte[length];
        ByteBuffer buf = ByteBuffer.wrap(payload);
        for (Map.Entry<Integer, Integer> entry : parameterMap.entrySet()) {
            buf.putShort(entry.getKey().shortValue());
            buf.putInt(entry.getValue());
        }
        return payload;
    }
}
