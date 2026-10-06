package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.util.Objects;

/** A null score means ungraded; zero is a real, graded score. */
public record GradeEntry(String id, String name, BigDecimal earned, BigDecimal possible) {
    public GradeEntry {
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("Grade entry name and ID cannot be blank.");
        Objects.requireNonNull(possible, "Maximum points are required");
        requireReasonable(possible);
        if (earned != null) requireReasonable(earned);
        if (possible.signum() <= 0) throw new IllegalArgumentException("Maximum points must be greater than 0.");
        if (earned != null && (earned.signum() < 0 || earned.compareTo(possible) > 0))
            throw new IllegalArgumentException("Score must be between 0 and the maximum; leave it blank if ungraded.");
        name = name.strip();
    }

    static void requireReasonable(BigDecimal value) {
        if (value.precision() > 40 || Math.abs((long) value.scale()) > 60)
            throw new IllegalArgumentException("The number has excessive precision or magnitude.");
    }
}
