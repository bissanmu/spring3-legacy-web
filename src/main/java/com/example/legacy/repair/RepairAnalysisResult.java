package com.example.legacy.repair;

import java.util.Collections;
import java.util.List;

public class RepairAnalysisResult {

    private final String inputText;
    private final List<String> originalItems;
    private final List<RepairAnalysisItem> items;
    private final List<String> unmappedItems;
    private final RepairFeature feature;
    private final int dictionarySize;
    private final int mappedCount;
    private final int heuristicCount;
    private final boolean truncated;
    private final int truncatedItemCount;

    public RepairAnalysisResult(String inputText,
                                List<String> originalItems,
                                List<RepairAnalysisItem> items,
                                List<String> unmappedItems,
                                RepairFeature feature,
                                int dictionarySize,
                                boolean truncated,
                                int truncatedItemCount) {
        this.inputText = inputText;
        this.originalItems = Collections.unmodifiableList(originalItems);
        this.items = Collections.unmodifiableList(items);
        this.unmappedItems = Collections.unmodifiableList(unmappedItems);
        this.feature = feature;
        this.dictionarySize = dictionarySize;
        this.truncated = truncated;
        this.truncatedItemCount = truncatedItemCount;

        int mapped = 0;
        for (RepairAnalysisItem item : items) {
            if (item.isDictionaryMatched()) {
                mapped++;
            }
        }
        this.mappedCount = mapped;
        this.heuristicCount = items.size() - mapped;
    }

    public String getInputText() {
        return inputText;
    }

    public List<String> getOriginalItems() {
        return originalItems;
    }

    public List<RepairAnalysisItem> getItems() {
        return items;
    }

    public List<String> getUnmappedItems() {
        return unmappedItems;
    }

    public RepairFeature getFeature() {
        return feature;
    }

    public int getDictionarySize() {
        return dictionarySize;
    }

    public int getMappedCount() {
        return mappedCount;
    }

    public int getHeuristicCount() {
        return heuristicCount;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public int getTruncatedItemCount() {
        return truncatedItemCount;
    }
}
