package com.digdes.school;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Patterns {
    INSERT("(INSERT|insert|Insert)"),
    UPDATE("(UPDATE|update|Update)"),
    DELETE("(DELETE|delete|Delete)"),
    SELECT("(SELECT|select|Select)"),
    WHERE("(WHERE|where|Where)"),
    VALUES("(VALUES|values|Values)"),
    LASTNAME("'(LASTNAME|lastName|LastName|lastname|Lastname)'"),
    ID("'(ID|id|Id)'"),
    AGE("'(AGE|age|Age)'"),
    ACTIVE("'(ACTIVE|active)'"),
    NULL("(null|NULL)"),
    LONG_VALUE("\\d+"),
    COST("'(COST|cost|Cost)'"),
    DOUBLE_VALUE("\\d+.\\d+"),
    STRING_VALUE("(%|)([A-z\\d]+|[À-ÿ\\d]+)(%|)"),
    EQUALS("="),
    GREATER(">"),
    LESS_THAN("<"),
    GREATER_OR_EQUALS(">="),
    LESS_OR_EQUALS("<="),
    NOT_EQUALS("!="),
    LIKE("(like|LIKE|Like)"),
    I_LIKE("(ilike|iLIKE|iLike|Ilike)"),
    AND("(AND|and)"),
    OR("(OR|or)"),
    ANY_OPERATOR("((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))"),
    ANY_VALUE("(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True))"),
    ANY_COLUMN_NAME("('LASTNAME'|'lastname'|'lastName'|'Lastname'|'LastName')|('ID'|'id'|'Id')|('AGE'|'age'|Age')|('COST'|'cost'|'Cost')|('ACTIVE'|'active'|'Active')"),
    ANOTHER_COLUMN_NAME("('[A-z]+')"),
    IN_PARENTHESES("\\(.+\\)"),
    WHITESPACE("."),
    SINGLE_EXPRESSION("((('LASTNAME'|'lastname'|'lastName'|'Lastname'|'LastName')|('ID'|'id'|'Id')|('AGE'|'age'|Age')|('COST'|'cost'|'Cost')|('ACTIVE'|'active'|'Active'))((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True)))"),
    EXPRESSION("((result\\d)|(('LASTNAME'|'lastname'|'lastName'|'Lastname'|'LastName')|('ID'|'id'|'Id')|('AGE'|'age'|Age')|('COST'|'cost'|'Cost')|('ACTIVE'|'active'|'Active'))((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True))).(OR|or).((result\\d)|('[A-z]+')((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True)))"),
    PRIORITY_EXPRESSION("((result\\d)|(('LASTNAME'|'lastname'|'lastName'|'Lastname'|'LastName')|('ID'|'id'|'Id')|('AGE'|'age'|Age')|('COST'|'cost'|'Cost')|('ACTIVE'|'active'|'Active'))((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True))).(AND|and).((result\\d)|('[A-z]+')((.|)(i|I|)(like|LIKE|Like|=|!=|<=|>=|<|>)(|.))(.|)(%|)((\\d+.\\d+)|\\d+|(null|NULL)|([A-z\\d\\s]+|[À-ÿ\\d\\s]+)\\b(%|)'|(false|FALSE|False|TRUE|true|True)))"),
    RESULT("result\\d+"),
    ;

    private final String pattern;

    Patterns(String s) {
        this.pattern = s;
    }

    private String getName() {
        return pattern;
    }

    public static Pattern getPattern(Patterns pattern) {
        return Pattern.compile(pattern.getName());
    }

    public static Matcher getMatcher(String name, CharSequence string) {
        return getPattern(Objects.requireNonNull(findPattern(name))).matcher(string);
    }

    public static Matcher getMatcher(Patterns pattern, CharSequence string) {
        return getPattern(pattern).matcher(string);
    }

    private static Matcher getGroupMatcher(String pattern, CharSequence string) {
        return Pattern.compile(pattern).matcher(string);
    }

    public static Patterns findPattern(String name) {
        for (Patterns pattern : Patterns.values()) {
            if (pattern.name().equals(name.toUpperCase())) {
                return pattern;
            }
        }
        return null;
    }

    public static Matcher getGroupMatcher(Queue<Patterns> patterns, CharSequence request) {
        StringBuilder groupPattern = new StringBuilder();
        while (!patterns.isEmpty()) {
            groupPattern = groupPattern.append(patterns.poll().getName());
        }
        return getGroupMatcher(String.valueOf(groupPattern), request);
    }

    public static Matcher getPriorityConditionPattern(CharSequence request) {
        Queue<Patterns> patterns = new ArrayDeque<>();
        patterns.add(Patterns.ANY_COLUMN_NAME);
        patterns.add(Patterns.ANY_OPERATOR);
        patterns.add(Patterns.ANY_VALUE);
        patterns.add(Patterns.WHITESPACE);
        patterns.add(Patterns.AND);
        patterns.add(Patterns.WHITESPACE);
        patterns.add(Patterns.ANY_COLUMN_NAME);
        patterns.add(Patterns.ANY_OPERATOR);
        patterns.add(Patterns.ANY_VALUE);
        return getGroupMatcher(patterns, request);
    }
}
