package org.ups.citamedicos.domain.valueobject;

import java.util.regex.Pattern;

public final class NumeroWhatsApp {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final String value;

    private NumeroWhatsApp(String value) {
        if (value == null || !E164.matcher(value).matches()) {
            throw new IllegalArgumentException("NumeroWhatsApp must be in E.164 format: " + value);
        }
        this.value = value;
    }

    public static NumeroWhatsApp of(String value) {
        return new NumeroWhatsApp(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumeroWhatsApp other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
