package org.laborercode.http2.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.encoders.UrlBase64;
import org.laborercode.http2.client.Http2Request.RequestMethod;

public class UpgradeRequest {
    private RequestMethod method;
    private String path;
    private Map<String, String> headers;
    private String data;

    private UpgradeRequest() {
        method = RequestMethod.GET;
        path = "/";
        headers = new HashMap<String, String>();
    }

    public static class Builder {
        private UpgradeRequest ur;

        public Builder() {
            ur = new UpgradeRequest();
        }

        public Builder method(RequestMethod method) {
            ur.method = method;
            return this;
        }

        public Builder path(String path) {
            ur.path = path;
            return this;
        }

        public Builder header(String name, String value) {
            ur.headers.put(name, value);
            return this;
        }

        public Builder data(String data) {
            ur.data = data;
            return this;
        }

        public UpgradeRequest build() {
            return ur;
        }
    }

    String getRequestString(String host, int port, Settings settings) throws IOException {
        String request = method.name() + " " + path + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Connection: Upgrade, HTTP2-Settings\r\n"
                + "Upgrade: h2c\r\n"
                + "HTTP2-Settings: " + encodedSettings(settings) + "\r\n";

        for(Map.Entry<String, String> entry : headers.entrySet()) {
            request += entry.getKey() + ":" + entry.getValue() + "\r\n";
        }
        request += "\r\n";

        if(method == RequestMethod.POST && data != null) {
            request += data;
        }
        return request;
    }

    private String encodedSettings(Settings settings) throws IOException {
        byte[] payload;
        if(settings == null) {
            settings = new Settings();
        }
        payload = settings.payload();

        int length = payload.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream(length);
        UrlBase64.encode(payload, out);

        return out.toString("ISO-8859-1");
    }
}
