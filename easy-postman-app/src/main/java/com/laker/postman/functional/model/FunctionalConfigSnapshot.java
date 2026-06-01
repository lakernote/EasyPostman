package com.laker.postman.functional.model;

import lombok.Value;

import java.util.List;

@Value
public class FunctionalConfigSnapshot {
    List<FunctionalConfigRow> rows;
    FunctionalCsvDataState csvState;

    public FunctionalConfigSnapshot(List<FunctionalConfigRow> rows, FunctionalCsvDataState csvState) {
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        this.csvState = csvState;
    }

    public static FunctionalConfigSnapshot empty() {
        return new FunctionalConfigSnapshot(List.of(), null);
    }
}
