package com.laker.postman.performance.core.run;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class PerformanceRunPlanAssetScanner {

    public List<PerformanceRunAsset> scan(PerformanceCorePlanDocument document) {
        Map<String, PerformanceRunAsset> assets = new LinkedHashMap<>();
        scanNode(document == null ? null : document.getRoot(), assets);
        return new ArrayList<>(assets.values());
    }

    private void scanNode(PerformanceCorePlanNode node, Map<String, PerformanceRunAsset> assets) {
        if (node == null) {
            return;
        }
        if (node.getType() == NodeType.CSV_DATA_SET) {
            addCsvAsset(node.getCsvDataSetData(), assets);
        } else if (node.getType() == NodeType.REQUEST) {
            addRequestAssets(node.getRequestSnapshot(), assets);
        }
        for (PerformanceCorePlanNode child : node.getChildren()) {
            scanNode(child, assets);
        }
    }

    private void addCsvAsset(CsvDataSetData data, Map<String, PerformanceRunAsset> assets) {
        if (data == null || !data.hasFileReference()) {
            return;
        }
        addAsset(assets, PerformanceRunAsset.TYPE_CSV, data.getFilePath());
    }

    private void addRequestAssets(PerformanceRequestSnapshot snapshot, Map<String, PerformanceRunAsset> assets) {
        if (snapshot == null) {
            return;
        }
        for (PerformanceRequestFormDataPart part : snapshot.getFormData()) {
            if (part != null && part.isFile() && !part.getValue().isBlank()) {
                addAsset(assets, PerformanceRunAsset.TYPE_FILE, part.getValue());
            }
        }
    }

    private void addAsset(Map<String, PerformanceRunAsset> assets, String type, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        String key = type + ":" + path;
        assets.putIfAbsent(key, new PerformanceRunAsset(assetId(type, path), type, path, null));
    }

    private String assetId(String type, String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        String cleanPath = normalized.replaceAll("[^A-Za-z0-9._/-]", "-")
                .replace('/', '-');
        return type.toLowerCase() + "-" + cleanPath;
    }
}
