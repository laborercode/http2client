package org.laborercode.http2.client.alpn;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.ALPN.ClientProvider;

public class AlpnProvider implements ClientProvider {

    private SSLSocket socket;

    public AlpnProvider(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public List<String> protocols() {
        return Arrays.asList(new String[] { "h2" });
    }

    @Override
    public void selected(String arg0) throws SSLException {
        ALPN.remove(socket);
    }

    @Override
    public void unsupported() {
        ALPN.remove(socket);
    }

}
