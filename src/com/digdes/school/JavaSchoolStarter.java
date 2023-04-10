package com.digdes.school;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSchoolStarter {
    private List<Map<String, Object>> collection = new ArrayList<>();

    public JavaSchoolStarter() {
    }

    public List<Map<String, Object>> getCollection() {
        return collection;
    }

    public List<Map<String, Object>> execute(String request) throws Exception {
        for (Commands commands : Commands.values()) {
            Matcher matcher = Patterns.getMatcher(commands.name(), request);
            if (matcher.lookingAt() && (matcher.start() == 0)) {
                request = request.substring(matcher.end());
                switch (commands) {
                    case INSERT -> {
                        return insert(request);
                    }
                    case DELETE -> {
                        return delete(request);
                    }
                    case SELECT -> {
                        return select(request);
                    }
                    case UPDATE -> {
                        return update(request);
                    }
                }
            }
        }
        throw new Exception("Could not identify the command");
    }

    private List<Map<String, Object>> insert(String request) throws Exception {
        request = checkValuesOperator(request);
        Map<ColumnNames, Object> values = getValues(request);
        Map<String, Object> newObject = new HashMap<>();
        List<Map<String, Object>> newEntry = new ArrayList<>();

        for (ColumnNames name : ColumnNames.values()) {
            newObject.put(name.name(), values.get(name));
        }
        checkingForEmptyValues(newObject);
        collection.add(newObject);
        newEntry.add(newObject);
        return makeAnswer(newEntry);
    }

    private List<Map<String, Object>> update(String request) throws Exception {
        int whereEnds = checkWhereOperator(request);
        Map<ColumnNames, Object> values = getValues(checkValuesOperator(
                request.substring(0, whereEnds - 5)));
        List<Map<String, Object>> selectedList;
        if (whereEnds > 0) {
            selectedList = selectByCondition(request.substring(whereEnds));
        } else {
            selectedList = collection;
        }
        for (Map map : selectedList) {
            for (ColumnNames columnName : values.keySet()) {
                map.put(columnName.name(), values.get(columnName));
            }
            checkingForEmptyValues(map);
        }
        return makeAnswer(selectedList);
    }

    private List<Map<String, Object>> delete(String request) throws Exception {
        int whereEnds = checkWhereOperator(request);
        if (whereEnds < 0) {
            List<Map<String, Object>> answer = new ArrayList<>(collection);
            collection.clear();
            return makeAnswer(answer);
        }
        List<Map<String, Object>> selectedList = selectByCondition(request.substring(whereEnds));
        collection.removeAll(selectedList);
        return makeAnswer(selectedList);
    }

    private List<Map<String, Object>> select(String request) throws Exception {
        int whereEnds = checkWhereOperator(request);
        if (whereEnds > 0) {
            return makeAnswer(selectByCondition(request.substring(whereEnds)));
        }
        return makeAnswer(collection);
    }

    private String checkValuesOperator(String request) throws Exception {
        if (request.charAt(0) == ' ') {
            request = request.substring(1);
        }
        Matcher valuesMatcher = Patterns.getMatcher(Patterns.VALUES, String.valueOf(request));
        if (valuesMatcher.lookingAt() && valuesMatcher.start() == 0) {
            request = request.substring(valuesMatcher.end());
            return request;
        } else {
            throw new Exception("Incorrect input: expected 'VALUES', received: " + request.charAt(0));
        }
    }

    private int checkWhereOperator(String request) {
        Matcher matcher = Patterns.getMatcher(Patterns.WHERE, String.valueOf(request));
        if (matcher.find()) {
            return matcher.end();
        } else {
            return -1;
        }
    }

    private void checkingForEmptyValues(Map<String, Object> map) throws Exception {
        int nonEmptyValues = (int) map.values().stream().filter(Objects::nonNull).count();
        if (nonEmptyValues == 0) {
            throw new Exception("All values can't be empty");
        }
    }

    private List<Map<String, Object>> selection(Expression expression, Iterable<Map<String, Object>> list) throws Exception {
        List<Map<String, Object>> selectedList = new ArrayList<>();
        for (Map map : list) {
            if (expression.getComparisonOperator().compareTo(map.get(expression.getColumnName().name()),
                    expression.getValue())) {
                selectedList.add(map);
            }
        }
        return selectedList;
    }

    private Map<ColumnNames, Object> getValues(String request) throws Exception {
        Map<ColumnNames, Object> values = new HashMap<>();
        Matcher columnAndValueMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, request);
        while (columnAndValueMatcher.find()) {
            request = checkWhiteSpace(request);
            columnAndValueMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, request);
            if (columnAndValueMatcher.lookingAt() && columnAndValueMatcher.start() == 0) {
                String columnAndValueStr = request.substring(columnAndValueMatcher.start(), columnAndValueMatcher.end());
                request = request.substring(columnAndValueMatcher.end());
                Expression expression = readSingleExpression(columnAndValueStr);
                if (!expression.getComparisonOperator().equals(ComparisonOperator.EQUALS)) {
                    throw new Exception("To assign a value use '='");
                }
                values.put(expression.getColumnName(), expression.getValue());
                if (columnAndValueMatcher.find()) {
                    request = checkComma(request, columnAndValueStr);
                }
                columnAndValueMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, request);
            } else {
                checkTheRestOfRequest(request);
            }
        }
        checkTheRestOfRequest(request);
        return values;
    }

    private String checkWhiteSpace(String request) throws Exception {
        if (request.charAt(0) == ' ') {
            return request.substring(1);
        } else {
            throw new Exception("Unknown symbol " + request.charAt(0));
        }
    }

    private String checkComma(String request, String columnAndValueStr) throws Exception {
        if (request.charAt(0) == ',') {
            return request.substring(1);
        } else {
            throw new Exception("Missing ',' after " + columnAndValueStr);
        }
    }

    private void checkTheRestOfRequest(String restOfRequest) throws Exception {
        Matcher matcher = Patterns.getMatcher(Patterns.ANOTHER_COLUMN_NAME, restOfRequest);
        if (matcher.find()) {
            throw new Exception("Unknown column name " + restOfRequest.substring(matcher.start(), matcher.end()));
        } else if (!restOfRequest.isBlank()) {
            throw new Exception("Could not read: " + restOfRequest);
        }
    }

    private List<Map<String, Object>> selectByCondition(String conditionString) throws Exception {
        Map<Integer, List<Map<String, Object>>> results = new HashMap<>();
        int count = 1;
        Matcher singleExpressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, conditionString);
        while (singleExpressionMatcher.find(0)) {
            Matcher inParenthesesMatcher = Patterns.getMatcher(Patterns.IN_PARENTHESES, conditionString);
            if (inParenthesesMatcher.find(0)) {
                String name = "result" + count;
                String inParentheses = conditionString.substring(inParenthesesMatcher.start() + 1,
                        inParenthesesMatcher.end() - 1);
                conditionString = inParenthesesMatcher.replaceFirst(name);
                results.put(count, selectByCondition(inParentheses));
                count++;
            }
            Matcher priorityExpMatcher = Patterns.getMatcher(Patterns.PRIORITY_EXPRESSION, conditionString);
            if (priorityExpMatcher.find(0)) {
                String name = "result" + count;
                String priorityExpressionStr = conditionString.substring(priorityExpMatcher.start(),
                        priorityExpMatcher.end());
                conditionString = priorityExpMatcher.replaceFirst(name);
                int resultNumber = getResultNumber(priorityExpressionStr);
                results.put(count, readExpression(priorityExpressionStr, results));
                if (resultNumber > 0) {
                    results.remove(resultNumber);
                }
                count++;
                continue;
            }
            Matcher expressionMatcher = Patterns.getMatcher(Patterns.EXPRESSION, conditionString);
            if (expressionMatcher.find(0)) {
                String name = "result" + count;
                String expressionStr = conditionString.substring(expressionMatcher.start(), expressionMatcher.end());
                conditionString = expressionMatcher.replaceFirst(name);
                results.put(count, readExpression(expressionStr, results));
                count++;
            }
            singleExpressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, conditionString);
            if (singleExpressionMatcher.find(0)) {
                String name = "result" + count;
                String singleExpressionStr = conditionString.substring(singleExpressionMatcher.start(),
                        singleExpressionMatcher.end());
                conditionString = singleExpressionMatcher.replaceFirst(name);
                results.put(count, selection(readSingleExpression(singleExpressionStr), collection));
                count++;
            }
            singleExpressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, conditionString);
        }
        Matcher resultMatcher = Patterns.getMatcher(Patterns.RESULT, conditionString);
        if (resultMatcher.find()) {
            conditionString = conditionString.substring(resultMatcher.end());
        }
        checkTheRestOfRequest(conditionString);
        return calculateResult(results);
    }

    private List<Map<String, Object>> calculateResult(Map<Integer, List<Map<String, Object>>> results) {
        HashSet<Map<String, Object>> selectedObjects = new HashSet<>();
        for (List list : results.values()) {
            selectedObjects.addAll(list);
        }
        return selectedObjects.stream().toList();
    }

    private int getResultNumber(String s) {
        Matcher matcher = Patterns.getMatcher(Patterns.RESULT, s);
        if (matcher.find(0)) {
            s = s.substring(matcher.start(), matcher.end());
            Pattern pattern = Pattern.compile("\\d+");
            matcher = pattern.matcher(s);
            if (matcher.find(0)) {
                return Integer.parseInt(s.substring(matcher.start(), matcher.end()));
            }
        }
        return -1;
    }

    private List<Map<String, Object>> readExpression(String expressionStr,
                                                     Map<Integer, List<Map<String, Object>>> results) throws Exception {
        HashSet<Map<String, Object>> intermediateResult = new HashSet<>();
        HashSet<Map<String, Object>> finalResult = new HashSet<>();
        int resultNumber = getResultNumber(expressionStr);
        if (resultNumber > 0) {
            return readExpressionWithResult(expressionStr, results.get(resultNumber));
        }
        Matcher expressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, expressionStr);
        if (expressionMatcher.find(0)) {
            Expression expression = readSingleExpression(expressionStr.substring(expressionMatcher.start(),
                    expressionMatcher.end()));
            checkExpression(expression);
            intermediateResult.addAll(selection(expression, collection));
            expressionStr = expressionStr.substring(expressionMatcher.end());
            Matcher orMatcher = Patterns.getMatcher(Patterns.OR, expressionStr);
            expressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, expressionStr);
            if (orMatcher.find(0)) {
                if (expressionMatcher.find(0)) {
                    finalResult = intermediateResult;
                    finalResult.addAll(selection(readSingleExpression(
                            expressionStr.substring(expressionMatcher.start(), expressionMatcher.end())), collection));
                }
            }
            Matcher andMatcher = Patterns.getMatcher(Patterns.AND, expressionStr);
            if (andMatcher.find(0)) {
                if (expressionMatcher.find(0)) {
                    expressionStr = expressionStr.substring(andMatcher.end());
                    Expression expression1 = readSingleExpression(expressionStr);
                    checkExpression(expression1);
                    finalResult.addAll(selection(expression1, intermediateResult));
                }
            }
        }
        return finalResult.stream().toList();
    }

    private void checkExpression(Expression expression) throws Exception {
        if (expression.getValue() == null && !expression.getComparisonOperator().equals(ComparisonOperator.EQUALS)) {
            throw new Exception("It is not possible to compare the value in the column with null");
        }
    }

    private List<Map<String, Object>> readExpressionWithResult(String expressionStr,
                                                               List<Map<String, Object>> result) throws Exception {
        Matcher expressionMatcher = Patterns.getMatcher(Patterns.SINGLE_EXPRESSION, expressionStr);
        if (expressionMatcher.find(0)) {
            String singleExpressionStr = expressionStr.substring(expressionMatcher.start(),
                    expressionMatcher.end());
            Matcher andMatcher = Patterns.getMatcher(Patterns.AND, expressionStr);
            Matcher orMatcher = Patterns.getMatcher(Patterns.OR, expressionStr);
            if (andMatcher.find(0)) {
                Expression expression = readSingleExpression(singleExpressionStr);
                checkExpression(expression);
                return selection(expression, result);
            } else if (orMatcher.find(0)) {
                HashSet<Map<String, Object>> results = new HashSet<>();
                results.addAll(result);
                Expression expression1 = readSingleExpression(singleExpressionStr);
                checkExpression(expression1);
                results.addAll(selection(expression1, collection));
                return results.stream().toList();
            }
        }
        throw new Exception();
    }

    private Expression readSingleExpression(String conditionString) throws Exception {
        Matcher matcher = Patterns.getMatcher(Patterns.ANY_COLUMN_NAME, conditionString);
        for (ColumnNames name : ColumnNames.values()) {
            conditionString = conditionString.trim();
            if (matcher.find(0)) {
                String column = conditionString.substring(matcher.start(), matcher.end()).toUpperCase();
                if (column.contains(name.name().toUpperCase())) {
                    return getExpression(name, conditionString);
                }
            }

        }
        throw new Exception("Unknown column name");
    }

    private Expression getExpression(ColumnNames name, String expressionString) throws Exception {
        switch (name.getType()) {
            case "Long" -> {
                return new Expression(name,
                        getComparisonOperator("Long", expressionString), getLongValue(expressionString));
            }
            case "String" -> {
                return new Expression(name,
                        getComparisonOperator("String", expressionString), getStringValue(expressionString));
            }
            case "Double" -> {
                return new Expression(name,
                        getComparisonOperator("Double", expressionString), getDoubleValue(expressionString));
            }
            case "Boolean" -> {
                return new Expression(name,
                        getComparisonOperator("Boolean", expressionString), getBooleanValue(expressionString));
            }
            default -> throw new Exception("Incorrect input");
        }
    }

    private ComparisonOperator getComparisonOperator(String type, String request) throws Exception {
        for (ComparisonOperator operator : ComparisonOperator.values()) {
            Matcher comparisonOperatorMatcher = Patterns.getMatcher(operator.name(), request);
            if (comparisonOperatorMatcher.find()) {
                return operator;
            }
        }
        throw new Exception("Unknown comparison operator");
    }

    private String getStringValue(String request) {
        Matcher lastNameDeleteMatcher = Patterns.getMatcher(Patterns.LASTNAME, request);
        if (lastNameDeleteMatcher.find()) {
            request = request.substring(lastNameDeleteMatcher.end());
        }
        Matcher anyOperatorMatcher = Patterns.getMatcher(Patterns.ANY_OPERATOR, request);
        if (anyOperatorMatcher.find()) {
            request = request.substring(anyOperatorMatcher.end());
        }
        Matcher stringValueMatcher = Patterns.getMatcher("string_value", String.valueOf(request));
        Matcher nullMatcher = Patterns.getMatcher("null", String.valueOf(request));
        if (nullMatcher.find()) {
            return null;
        } else if (stringValueMatcher.find()) {
            return request.substring(stringValueMatcher.start(), stringValueMatcher.end());
        }
        return null;
    }

    private Long getLongValue(String request) throws Exception {
        Matcher longValueMatcher = Patterns.getMatcher("long_value", String.valueOf(request));
        Matcher nullMatcher = Patterns.getMatcher("null", String.valueOf(request));
        if (longValueMatcher.find()) {
            return Long.valueOf(request.substring(longValueMatcher.start(), longValueMatcher.end()));
        } else if (nullMatcher.find()) {
            return null;
        }
        throw new Exception("Incorrect input: unknown values type");
    }

    private Double getDoubleValue(String request) throws Exception {
        Matcher doubleValueMatcher = Patterns.getMatcher(Patterns.DOUBLE_VALUE, String.valueOf(request));
        Matcher nullMatcher = Patterns.getMatcher("null", String.valueOf(request));
        if (doubleValueMatcher.find()) {
            return Double.valueOf(request.substring(doubleValueMatcher.start(), doubleValueMatcher.end()));
        } else if (nullMatcher.find()) {
            return null;
        }
        throw new Exception("Incorrect input: unknown values type");
    }

    private Boolean getBooleanValue(String request) {
        Matcher matcher = Patterns.getMatcher(Patterns.ACTIVE, request);
        if (matcher.find()) {
            request = request.substring(matcher.end());
        }
        Matcher stringValueMatcher = Patterns.getMatcher("string_value", String.valueOf(request));
        Matcher nullMatcher = Patterns.getMatcher("null", String.valueOf(request));
        if (nullMatcher.find()) {
            return null;
        } else if (stringValueMatcher.find()) {
            return Boolean.valueOf(request.substring(stringValueMatcher.start(), stringValueMatcher.end()));
        }
        return null;
    }

    private List<Map<String, Object>> makeAnswer(List<Map<String, Object>> newEntries) {
        for (Map m : newEntries) {
            m.values().removeAll(Collections.singleton(null));
        }
        return newEntries;
    }
}

enum Commands {
    INSERT,
    UPDATE,
    DELETE,
    SELECT
}


enum ColumnNames {
    id("Long"),
    lastName("String"),
    age("Long"),
    cost("Double"),
    active("Boolean");

    private final String type;

    ColumnNames(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}


