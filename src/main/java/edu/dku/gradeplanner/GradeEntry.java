package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.util.Objects;

/** A null score means ungraded; zero is a real, graded score. */
public record GradeEntry(String id, String name, BigDecimal earned, BigDecimal possible) {
    public GradeEntry {
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("成绩名称和 ID 不能为空。");
        Objects.requireNonNull(possible, "满分不能为空");
        requireReasonable(possible);
        if (earned != null) requireReasonable(earned);
        if (possible.signum() <= 0) throw new IllegalArgumentException("满分必须大于 0。");
        if (earned != null && (earned.signum() < 0 || earned.compareTo(possible) > 0))
            throw new IllegalArgumentException("得分必须在 0 和满分之间；未评分请留空。");
        name = name.strip();
    }

    static void requireReasonable(BigDecimal value) {
        if (value.precision() > 40 || Math.abs((long) value.scale()) > 60)
            throw new IllegalArgumentException("数字精度或数量级过大。");
    }
}
