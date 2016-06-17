package org.laborercode.http2.client;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

class FrameWriter {
    private LinkedBlockingQueue<Frame> queue;
    private Http2FrameOutputStream out;

    public FrameWriter(Http2Client client) {
        queue = new LinkedBlockingQueue<Frame>(1024);
        out = new Http2FrameOutputStream(client.outputStream());
    }

    public void write(Frame frame) {
        try {
            queue.put(frame);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(new Task()).start();
    }

    private class Task implements Runnable {

        @Override
        public void run() {
            try {
                while(true) {
                    Frame frame = queue.take();

                    if(frame == null) {
                        
                    }
                    out.write((AbstractFrame)frame);
                }
            } catch(InterruptedException e) {
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
        }
    }
}
