package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/** Independent of Swing and file storage. All weights are entered as percentages. */
public final class GradeCalculator {
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private GradeCalculator() { }

    public record Result(BigDecimal required, BigDecimal minimum, BigDecimal maximum,
                         Map<String, BigDecimal> assumptions) {
        public Result { assumptions = Map.copyOf(assumptions); }
        public boolean reachable() { return required.compareTo(HUNDRED) <= 0; }
        public boolean zeroSuffices() { return required.signum() <= 0; }
        public String displayedRequirement() {
            return required.max(BigDecimal.ZERO).setScale(2, RoundingMode.CEILING).toPlainString();
        }
    }

    public static Optional<BigDecimal> average(GradeCategory category) {
        var graded = category.entries().stream().filter(e -> e.earned() != null).toList();
        if (graded.isEmpty()) return Optional.empty();
        BigDecimal total = BigDecimal.ZERO;
        if (category.mode() == GradeCategory.Mode.EQUAL) {
            for (var e : graded) total = total.add(e.earned().multiply(HUNDRED).divide(e.possible(), MC));
            return Optional.of(total.divide(BigDecimal.valueOf(graded.size()), MC));
        }
        BigDecimal possible = BigDecimal.ZERO;
        for (var e : graded) {
            total = total.add(e.earned());
            possible = possible.add(e.possible());
        }
        return Optional.of(total.multiply(HUNDRED).divide(possible, MC));
    }

    public static BigDecimal totalWeight(Course course) {
        return course.categories().stream().map(GradeCategory::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal finalizedContribution(Course course) {
        BigDecimal total = BigDecimal.ZERO;
        for (var category : course.categories())
            if (category.finalized()) total = total.add(contribution(average(category).orElseThrow(), category.weight()));
        return total;
    }

    public static Optional<BigDecimal> finalizedAverage(Course course) {
        var weight = course.categories().stream().filter(GradeCategory::finalized)
                .map(GradeCategory::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        return weight.signum() == 0 ? Optional.empty()
                : Optional.of(finalizedContribution(course).multiply(HUNDRED).divide(weight, MC));
    }

    public static boolean canSolve(GradeCategory c) {
        return !c.finalized() && c.weight().signum() > 0 && c.entries().size() == 1
                && c.entries().getFirst().earned() == null;
    }

    public static Result requiredScore(Course course, String categoryId, BigDecimal target,
                                       Map<String, BigDecimal> expected) {
        checkPercent(target, "目标总分");
        if (totalWeight(course).compareTo(HUNDRED) != 0)
            throw new IllegalArgumentException("类别权重合计必须为 100%，当前为 " + totalWeight(course).toPlainString() + "%。");
        var exam = course.categories().stream().filter(c -> c.id().equals(categoryId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请选择待反推考试。"));
        if (!canSolve(exam)) throw new IllegalArgumentException("待反推类别须有且仅有一条未评分成绩、权重大于 0，且尚未结束。");
        var assumptions = new java.util.LinkedHashMap<String, BigDecimal>();
        BigDecimal other = BigDecimal.ZERO;
        for (var c : course.categories()) {
            if (c.id().equals(exam.id()) || c.weight().signum() == 0) continue;
            BigDecimal score;
            if (c.finalized()) score = average(c).orElseThrow();
            else {
                score = expected.get(c.id());
                checkPercent(score, c.name() + " 的预计最终平均分");
                assumptions.put(c.id(), score);
            }
            other = other.add(contribution(score, c.weight()));
        }
        var required = target.subtract(other).multiply(HUNDRED).divide(exam.weight(), MC);
        return new Result(required, other, other.add(exam.weight()), assumptions);
    }

    private static BigDecimal contribution(BigDecimal score, BigDecimal weight) {
        return score.multiply(weight).divide(HUNDRED, MC);
    }

    public static void checkPercent(BigDecimal value, String field) {
        if (value == null) throw new IllegalArgumentException("请填写 " + field + "。");
        GradeEntry.requireReasonable(value);
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0)
            throw new IllegalArgumentException(field + " 必须在 0–100 之间。");
    }
}
