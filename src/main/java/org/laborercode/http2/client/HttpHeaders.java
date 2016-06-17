package org.laborercode.http2.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HttpHeaders {
    private HashMap<String, Set<String>> headerMap;

    public HttpHeaders() {
        headerMap = new HashMap<String, Set<String>>();
    }

    public HttpHeaders add(String key, String value) {
        Set<String> values = headerMap.get(key);
        if(values == null) {
            values = new HashSet<String>();
            headerMap.put(key, values);
        }
        values.add(value);
        return this;
    }

    public Set<String> get(String key) {
        return headerMap.get(key);
    }

    public HttpHeaders remove(String key, String value) {
        Set<String> valueSet = headerMap.remove(key);
        valueSet.remove(value);
        return this;
    }
    
    public Map<String, Set<String>> headerMap() {
        return headerMap;
    }
}
