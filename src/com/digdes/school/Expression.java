package com.digdes.school;

public class Expression<T> {
    private final ColumnNames columnName;
    private final ComparisonOperator comparisonOperator;
    private final T value;

    public <value> Expression(ColumnNames columnName, ComparisonOperator comparisonOperator, T value) {
        this.columnName = columnName;
        this.comparisonOperator = comparisonOperator;
        this.value = value;
    }
    public ColumnNames getColumnName() {
        return columnName;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public T getValue() {
        return value;
    }
}
