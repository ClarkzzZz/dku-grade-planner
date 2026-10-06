package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record GradeCategory(String id, String name, BigDecimal weight, Mode mode,
                            boolean finalized, List<GradeEntry> entries) {
    public enum Mode {
        EQUAL("每次等权"), POINTS("按总点数");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public GradeCategory {
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("类别名称和 ID 不能为空。");
        Objects.requireNonNull(weight, "权重不能为空");
        GradeEntry.requireReasonable(weight);
        Objects.requireNonNull(mode, "请选择汇总方式");
        if (weight.signum() < 0 || weight.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("权重必须在 0–100% 之间。");
        entries = List.copyOf(entries);
        if (entries.size() > 1000) throw new IllegalArgumentException("每个类别最多 1000 条成绩。");
        var ids = new HashSet<String>();
        for (var entry : entries)
            if (!ids.add(entry.id())) throw new IllegalArgumentException("成绩条目 ID 重复。");
        if (finalized && (entries.isEmpty() || entries.stream().anyMatch(e -> e.earned() == null)))
            throw new IllegalArgumentException("类别只有在至少一条成绩且全部已评分时才能结束。");
        name = name.strip();
    }

    public GradeCategory withEntries(List<GradeEntry> updated) {
        return new GradeCategory(id, name, weight, mode, finalized, updated);
    }
}
