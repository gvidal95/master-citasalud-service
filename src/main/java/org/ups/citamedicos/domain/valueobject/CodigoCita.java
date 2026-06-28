package org.ups.citamedicos.domain.valueobject;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CodigoCita {

    private static final Pattern UUID_V4 =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    private final String value;

    private CodigoCita(String value) {
        if (value == null || !UUID_V4.matcher(value).matches()) {
            throw new IllegalArgumentException("CodigoCita must be a valid UUID v4: " + value);
        }
        this.value = value;
    }

    public static CodigoCita generate() {
        return new CodigoCita(UUID.randomUUID().toString());
    }

    public static CodigoCita of(String value) {
        return new CodigoCita(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodigoCita other)) return false;
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
