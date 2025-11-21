package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 数据处理工具类
 */
@Slf4j
@UtilityClass
public class CsvDataUtil {

    /**
     * 读取 CSV 文件数据
     *
     * @param csvFile CSV 文件
     * @return 数据列表，每行数据用 Map 表示
     */
    public static List<Map<String, String>> readCsvData(File csvFile) {
        if (csvFile == null || !csvFile.exists() || !csvFile.isFile()) {
            log.warn("CSV 文件不存在或无效: {}", csvFile);
            return new ArrayList<>();
        }

        try {
            String content = FileUtil.readString(csvFile, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                log.warn("CSV 文件内容为空: {}", csvFile.getAbsolutePath());
                return new ArrayList<>();
            }

            CsvReader reader = CsvUtil.getReader();
            CsvData csvData = reader.readFromStr(content);
            List<CsvRow> rows = csvData.getRows();

            if (rows.isEmpty()) {
                log.warn("CSV 文件没有数据行: {}", csvFile.getAbsolutePath());
                return new ArrayList<>();
            }

            // 第一行作为列头
            List<String> headers = rows.get(0);
            List<Map<String, String>> dataList = new ArrayList<>();

            // 从第二行开始处理数据
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                // 使用LinkedHashMap保持列的顺序
                Map<String, String> rowData = new LinkedHashMap<>();

                for (int j = 0; j < headers.size() && j < row.size(); j++) {
                    String header = headers.get(j).trim();
                    String value = row.get(j);
                    rowData.put(header, value);
                }

                dataList.add(rowData);
            }

            log.debug("成功读取 CSV 文件: {}, 数据行数: {}", csvFile.getAbsolutePath(), dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("读取 CSV 文件失败: {}", csvFile.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 验证 CSV 文件格式
     *
     * @param csvFile CSV 文件
     * @return 验证结果信息
     */
    public static String validateCsvFile(File csvFile) {
        if (csvFile == null || !csvFile.exists()) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_EXIST);
        }

        if (!csvFile.isFile()) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_VALID);
        }

        if (!csvFile.getName().toLowerCase().endsWith(".csv")) {
            return I18nUtil.getMessage(MessageKeys.CSV_FILE_NOT_CSV);
        }

        try {
            List<Map<String, String>> data = readCsvData(csvFile);
            if (data.isEmpty()) {
                return I18nUtil.getMessage(MessageKeys.CSV_NO_VALID_DATA);
            }

            return "";
        } catch (Exception e) {
            return I18nUtil.getMessage(MessageKeys.CSV_LOAD_FAILED, e.getMessage());
        }
    }

    /**
     * 获取 CSV 文件的列头信息
     *
     * @param csvFile CSV 文件
     * @return 列头列表
     */
    public static List<String> getCsvHeaders(File csvFile) {
        try {
            String content = FileUtil.readString(csvFile, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return new ArrayList<>();
            }

            CsvReader reader = CsvUtil.getReader();
            CsvData csvData = reader.readFromStr(content);
            List<CsvRow> rows = csvData.getRows();

            if (rows.isEmpty()) {
                return new ArrayList<>();
            }

            return rows.get(0);
        } catch (Exception e) {
            log.error("获取 CSV 列头失败: {}", csvFile.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }
}
