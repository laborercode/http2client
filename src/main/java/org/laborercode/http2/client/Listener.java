package org.laborercode.http2.client;

public interface Listener {

    public void onHeaders(Stream stream, Frame frame);

    public void onData(Stream stream, Frame frame);

    public void onPriority(Stream stream, Frame frame);

    public void onPush(Stream stream, Frame frame);

    public void onReset(Stream stream, Frame frame);

    public void onWindow(Stream stream, Frame frame);

    public void onPing(Stream stream, Frame frame);

    public void onSettings(Stream stream, Frame frame);

    public void onGoaway(Stream stream, Frame frame);

    public void onContinuation(Stream stream, Frame frame);
}
