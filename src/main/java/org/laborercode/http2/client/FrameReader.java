package org.laborercode.http2.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.twitter.hpack.Decoder;

class FrameReader {
    private ConcurrentHashMap<Integer, AbstractStream> streamMap;
    private InputStream in;
    private OutputStream out;
    private Decoder decoder;
    private Settings serverSettings;
    private AtomicInteger streamCounter;
    private int maxConcurrentStreams;
    private Http2Client client;

    private final static byte[] settingsAck = { 0, 0, 0, 4, 1, 0, 0, 0, 0 };


    FrameReader(Http2Client client) {
        this.client = client;
        this.in = client.inputStream();
        this.out = client.outputStream();

        serverSettings = client.serverSettings();
        this.decoder = new Decoder(
                serverSettings.getSetting(Settings.SETTINGS_MAX_HEADER_LIST_SIZE),
                serverSettings.getSetting(Settings.SETTINGS_HEADER_TABLE_SIZE));

        streamCounter = new AtomicInteger(0);
        streamMap = new ConcurrentHashMap<Integer, AbstractStream>();

        ConnectionStream connectionStream = new ConnectionStream(client);
        connectionStream.listener(new ConnectionStreamListener(client.testListener()));
        streamMap.put(0, connectionStream);
    }

    public void add(int streamId, AbstractStream stream) {
        synchronized(streamCounter) {
            while(streamCounter.get() >= maxConcurrentStreams) {
                try {
                    streamCounter.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            streamCounter.incrementAndGet();
        }
        stream.decoder(decoder);
        streamMap.put(streamId, stream);
    }

    public void start() {
        new Thread(new Task()).start();
    }

    private class Task implements Runnable {
        @Override
        public void run() {
            byte[] header;
            byte[] payload;

            try {
                while (true) {
                    int offset, remaining;
                    int byteRead;

                    header = new byte[Http2Constants.FRAME_HEADER_SIZE];
                    offset = 0; remaining = 9;
                    while(remaining > 0) {
                        byteRead = in.read(header, offset, remaining);
                        offset += byteRead;
                        remaining -= byteRead;
                    }

                    AbstractFrame frame = FrameBuilder.build(header);
                    int payloadLength = frame.length();
                    if (payloadLength > 0) {
                        offset = 0;
                        remaining = frame.length();
                        payload = new byte[payloadLength];
                        while(remaining > 0) {
                            byteRead = in.read(payload, offset, remaining);
                            offset += byteRead;
                            remaining -= byteRead;
                        }
                        frame.payload(payload);
                    }

                    AbstractStream stream = streamMap.get(frame.streamId());
                    if(stream == null) {
                        // end stream or invalid stream id
                        continue;
                    }
                    frame.process(stream);

                    if(frame.isEndStream()) {
                        synchronized(streamCounter) {
                            streamCounter.decrementAndGet();
                            streamCounter.notifyAll();
                        }
                        streamMap.remove(frame.streamId());
                    }

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ConnectionStreamListener implements Listener {
        private ConnectionListener listener;

        ConnectionStreamListener(ConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onHeaders(Stream stream, Frame frame) {
        }

        @Override
        public void onData(Stream stream, Frame frame) {
        }

        @Override
        public void onPriority(Stream stream, Frame frame) {
        }

        @Override
        public void onContinuation(Stream stream, Frame frame) {
        }

        @Override
        public void onReset(Stream stream, Frame frame) {
        }

        @Override
        public void onWindow(Stream stream,Frame frame) {
        }

        @Override
        public void onPing(Stream stream, Frame frame) {
            if(!frame.isAck()) {
                // send pong
                byte[] data = frame.payload();
                stream.ping(data, true);
            }
            if(listener != null) {
                listener.onPing(stream, frame);
            }
        }

        @Override
        public void onSettings(Stream stream, Frame frame) {
            if(frame.isAck()) {
                // do nothing yet...
            } else {
                try {
                    Settings newSettings = new Settings(frame.payload());
                    int newStreams = newSettings.getSetting(Settings.SETTINGS_MAX_CONCURRENT_STREAMS);
                    if(newStreams > maxConcurrentStreams) {
                        synchronized(streamCounter) {
                            streamCounter.notifyAll();
                        }
                    }
                    maxConcurrentStreams = newStreams;
                    // set decoder

                    int oldValue = decoder.getMaxHeaderTableSize();
                    int newValue = newSettings.getSetting(Settings.SETTINGS_HEADER_TABLE_SIZE);
                    if(oldValue != newValue) {
                        decoder.setMaxHeaderTableSize(newValue);
                    }
                    serverSettings.update(newSettings);

                    out.write(settingsAck);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(listener != null) {
                listener.onSettings(stream, frame);
            }
        }

        @Override
        public void onGoaway(Stream stream, org.laborercode.http2.client.Frame frame) {
            if(listener != null) {
                listener.onGoaway(stream, frame);
            }
        }

        @Override
        public void onPush(Stream stream, Frame frame) {
        }
    }
}
