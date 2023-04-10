package com.digdes.school;

import java.util.List;

public enum ComparisonOperator {
    GREATER_OR_EQUALS(List.of("Long", "Double")),
    LESS_OR_EQUALS(List.of("Long", "Double")),
    NOT_EQUALS(List.of("Boolean", "String", "Long", "Double")),
    EQUALS(List.of("Boolean", "String", "Long", "Double")),
    GREATER(List.of("Long", "Double")),
    LESS_THAN(List.of("Long", "Double")),
    I_LIKE(List.of("String")),
    LIKE(List.of("String"));

    private List<String> availableTypes;

    ComparisonOperator(List<String> availableTypes) {
        this.availableTypes = availableTypes;
    }

    public boolean compareTo(Object o1, Object o2) throws Exception {
        if (o1 == null) {
            return false;
        }
        typeValidator(o1, o2);
        switch (this) {
            case EQUALS -> {
                return o1.equals(o2);
            }
            case NOT_EQUALS -> {
                return !o1.equals(o2);
            }
            case GREATER, LESS_THAN, GREATER_OR_EQUALS, LESS_OR_EQUALS -> {
                return doubleAndLongComparator(o1, o2);
            }
            case LIKE -> {
                return like((String) o1, (String) o2);
            }
            case I_LIKE -> {
                return iLike((String) o1, (String) o2);
            }
        }
        return false;
    }

    private boolean doubleAndLongComparator(Object o1, Object o2) throws Exception {
        boolean result;
        switch (o1.getClass().getSimpleName()) {
            case "Long" -> {
                return compareLong(o1, o2);
            }
            case "Double" -> {
                return compareDouble(o1, o2);
            }
            default -> throw new Exception("Unknown types");
        }
    }

    private boolean compareLong(Object o1, Object o2) throws Exception {
        Long l1 = (Long) o1;
        Long l2 = (Long) o2;
        switch (this) {
            case GREATER -> {
                return l1 > l2;
            }
            case LESS_THAN -> {
                return l1 < l2;
            }
            case GREATER_OR_EQUALS -> {
                return l1 >= l2;
            }
            case LESS_OR_EQUALS -> {
                return l1 <= l2;
            }
            default -> throw new Exception("Unknown types");
        }
    }

    private boolean compareDouble(Object o1, Object o2) throws Exception {
        Double d1 = (Double) o1;
        Double d2 = (Double) o2;
        switch (this) {
            case GREATER -> {
                return d1 > d2;
            }
            case LESS_THAN -> {
                return d1 < d2;
            }
            case GREATER_OR_EQUALS -> {
                return d1 >= d2;
            }
            case LESS_OR_EQUALS -> {
                return d1 <= d2;
            }
            default -> throw new Exception("Unknown types");
        }
    }

    private boolean like(String s1, String s2) {
        if (s1.equals(s2)) {
            return true;
        } else if (s2.startsWith("%") && s2.endsWith("%")
                                && s1.contains(s2.substring(1, s2.length() - 1))) {
            return true;
        } else if (s2.startsWith("%") && s1.endsWith(s2.substring(1))) {
            return true;
        } else if (s2.endsWith("%") && s1.startsWith(s2.substring(0, s2.length() - 1))) {
            return true;
        }
        return false;
    }

    private boolean iLike(String s1, String s2) {
        return like(s1.toUpperCase(), s2.toUpperCase());
    }

    public List<String> getAvailableTypes() {
        return availableTypes;
    }

    private void typeValidator(Object o1, Object o2) throws Exception {
        if (!availableTypes.contains(o1.getClass().getSimpleName()) &&
                !availableTypes.contains(o2.getClass().getSimpleName())) {
            throw new Exception("Unsupported type");
        }
        if (!o1.getClass().equals(o2.getClass())) {
            throw new Exception("The values being compared must be of the same type");
        }
    }
}
