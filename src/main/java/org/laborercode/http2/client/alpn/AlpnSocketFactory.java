package org.laborercode.http2.client.alpn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.jetty.alpn.ALPN;

public class AlpnSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory factory;
    private SSLSocket socket;

    public AlpnSocketFactory() {
        this.factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        socket = (SSLSocket)factory.createSocket(host, port);

        ALPN.put(socket, new AlpnProvider(socket));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        socket = (SSLSocket)factory.createSocket(host, port);

        ALPN.put(socket, new AlpnProvider(socket));
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        socket = (SSLSocket)factory.createSocket(host, port, localHost, localPort);

        ALPN.put(socket, new AlpnProvider(socket));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        socket = (SSLSocket)factory.createSocket(address, port, localAddress, localPort);

        ALPN.put(socket, new AlpnProvider(socket));
        return socket;
    }

}
