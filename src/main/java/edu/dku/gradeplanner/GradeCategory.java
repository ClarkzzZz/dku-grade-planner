package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record GradeCategory(String id, String name, BigDecimal weight, Mode mode,
                            boolean finalized, List<GradeEntry> entries) {
    public enum Mode {
        EQUAL("Equal Weight"), POINTS("Total Points");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public GradeCategory {
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("Category name and ID cannot be blank.");
        Objects.requireNonNull(weight, "Weight is required");
        GradeEntry.requireReasonable(weight);
        Objects.requireNonNull(mode, "Select an aggregation method");
        if (weight.signum() < 0 || weight.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Weight must be between 0 and 100%.");
        entries = List.copyOf(entries);
        if (entries.size() > 1000) throw new IllegalArgumentException("A category can have at most 1,000 entries.");
        var ids = new HashSet<String>();
        for (var entry : entries)
            if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate grade entry ID.");
        if (finalized && (entries.isEmpty() || entries.stream().anyMatch(e -> e.earned() == null)))
            throw new IllegalArgumentException("To finalize a category, add at least one entry and grade all entries.");
        name = name.strip();
    }

    public GradeCategory withEntries(List<GradeEntry> updated) {
        return new GradeCategory(id, name, weight, mode, finalized, updated);
    }
}
