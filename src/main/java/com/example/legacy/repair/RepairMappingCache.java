package com.example.legacy.repair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RepairMappingCache {

    private static final String DEFAULT_RESOURCE_PATH = "data/repair_mapping_dictionary.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Map<String, RepairMapping> cache = Collections.emptyMap();
    private volatile int dictionarySize;
    private String resourcePath = DEFAULT_RESOURCE_PATH;

    public RepairMappingCache() {
    }

    public RepairMappingCache(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public synchronized void reload() throws IOException {
        InputStream inputStream = openResource(resourcePath);
        try {
            List<RepairMapping> rows = objectMapper.readValue(inputStream, new TypeReference<List<RepairMapping>>() {
            });
            Map<String, RepairMapping> exact = new HashMap<String, RepairMapping>();
            Map<String, RepairMapping> next = new HashMap<String, RepairMapping>();
            int activeCount = 0;
            for (RepairMapping row : rows) {
                if (row == null || !row.isActive()) {
                    continue;
                }
                activeCount++;

                String key = row.getNormalizedKey();
                if (key == null || key.trim().length() == 0) {
                    key = RepairTextNormalizer.normalizeKey(row.getRawName());
                }
                if (key.length() > 0) {
                    exact.put(key, row);
                }
            }

            next.putAll(exact);
            for (RepairMapping row : rows) {
                if (row == null || !row.isActive()) {
                    continue;
                }
                for (String key : RepairTextNormalizer.lookupKeys(row.getRawName())) {
                    if (!exact.containsKey(key)) {
                        next.put(key, row);
                    }
                }
            }
            cache = Collections.unmodifiableMap(next);
            dictionarySize = activeCount;
        } finally {
            inputStream.close();
        }
    }

    public RepairMapping find(String rawName) {
        if (rawName == null) {
            return null;
        }
        for (String key : RepairTextNormalizer.lookupKeys(rawName)) {
            RepairMapping mapping = cache.get(key);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    public int size() {
        return dictionarySize;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    private InputStream openResource(String path) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            inputStream = RepairMappingCache.class.getClassLoader().getResourceAsStream(path);
        }
        if (inputStream == null) {
            throw new IOException("Repair mapping dictionary not found on classpath: " + path);
        }
        return inputStream;
    }
}
