package dev.shadowmx.gearsense;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class VersionNumber implements Comparable<VersionNumber> {
    private final List<BigInteger> numbers;
    private final String qualifier;

    private VersionNumber(List<BigInteger> numbers, String qualifier) {
        this.numbers = numbers;
        this.qualifier = qualifier;
    }

    static VersionNumber parse(String value) {
        String normalized = value == null ? "0" : value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        String[] versionAndQualifier = normalized.split("-", 2);
        String[] components = versionAndQualifier[0].split("\\.");
        List<BigInteger> numbers = new ArrayList<>();
        for (String component : components) {
            String digits = component.replaceAll("[^0-9].*$", "");
            numbers.add(digits.isEmpty() ? BigInteger.ZERO : new BigInteger(digits));
        }
        if (numbers.isEmpty()) numbers.add(BigInteger.ZERO);
        String qualifier = versionAndQualifier.length == 2
                ? versionAndQualifier[1].toLowerCase(Locale.ROOT)
                : "";
        return new VersionNumber(numbers, qualifier);
    }

    @Override
    public int compareTo(VersionNumber other) {
        int length = Math.max(numbers.size(), other.numbers.size());
        for (int index = 0; index < length; index++) {
            BigInteger left = index < numbers.size() ? numbers.get(index) : BigInteger.ZERO;
            BigInteger right = index < other.numbers.size() ? other.numbers.get(index) : BigInteger.ZERO;
            int result = left.compareTo(right);
            if (result != 0) return result;
        }
        if (qualifier.isEmpty() && !other.qualifier.isEmpty()) return 1;
        if (!qualifier.isEmpty() && other.qualifier.isEmpty()) return -1;
        return qualifier.compareTo(other.qualifier);
    }
}
