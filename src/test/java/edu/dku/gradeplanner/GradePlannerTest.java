package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** No third-party testing dependency. Run with scripts/test.sh. */
public final class GradePlannerTest {
    private static int checks;
    private static BigDecimal n(String value) { return new BigDecimal(value); }
    private static void check(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
    }
    private static void close(BigDecimal actual, String expected, String name) {
        check(actual.subtract(n(expected)).abs().compareTo(n("0.00000001")) < 0, name + ": " + actual);
    }
    private interface CheckedAction { void run() throws Exception; }
    private static void rejects(CheckedAction action, Class<? extends Exception> type, String name) throws Exception {
        try { action.run(); }
        catch (Exception e) { check(type.isInstance(e), name + ": wrong error " + e); return; }
        throw new AssertionError(name + ": invalid input accepted");
    }

    public static void main(String[] args) throws Exception {
        var demo = DemoData.create();
        var hw = demo.categories().getFirst();
        close(GradeCalculator.average(hw).orElseThrow(), "85", "Equal weighting excludes blank");
        var points = new GradeCategory(hw.id(), hw.name(), hw.weight(), GradeCategory.Mode.POINTS, false, hw.entries());
        close(GradeCalculator.average(points).orElseThrow(), "89.0909090909", "Total points weighting");
        var zero = new GradeCategory("z", "零分", n("10"), GradeCategory.Mode.EQUAL, false, List.of(
                new GradeEntry("z1", "零分", BigDecimal.ZERO, n("10")),
                new GradeEntry("z2", "未评分", null, n("100"))));
        close(GradeCalculator.average(zero).orElseThrow(), "0", "Zero counts; blank does not");
        var empty = new GradeCategory("e", "空", n("0"), GradeCategory.Mode.POINTS, false, List.of());
        check(GradeCalculator.average(empty).isEmpty(), "Empty category has no average");
        check(GradeCalculator.average(demo.categories().get(4)).isEmpty(), "All ungraded has no average");
        close(GradeCalculator.finalizedContribution(demo), "20", "Only finalized midterm contributes");
        close(GradeCalculator.finalizedAverage(demo).orElseThrow(), "80", "Finalized average denominator");
        check(GradeCalculator.finalizedAverage(new Course("空课程", List.of(empty))).isEmpty(), "No finalized weight");
        var expected = Map.of("homework", n("90"), "quiz", n("85"), "project", n("90"), "midterm", n("0"));
        var r = GradeCalculator.requiredScore(demo, "final", n("85"), expected);
        close(r.required(), "85", "Target calculation ignores prediction for finalized midterm");
        close(r.minimum(), "59.5", "Minimum total");
        close(r.maximum(), "89.5", "Maximum total");
        check(r.reachable() && !r.zeroSuffices() && r.displayedRequirement().equals("85.00"), "Normal classification");
        check(r.assumptions().size() == 3 && !r.assumptions().containsKey("midterm"), "Assumptions separate from actuals");
        var impossible = GradeCalculator.requiredScore(demo, "final", n("95"), expected);
        check(!impossible.reachable(), "Impossible target");
        check(GradeCalculator.requiredScore(demo, "final", n("55"), expected).zeroSuffices(), "Zero suffices");
        check(GradeCalculator.requiredScore(demo, "final", n("89.5"), expected).required().compareTo(n("100")) == 0, "Exact attainable maximum");
        check(GradeCalculator.requiredScore(demo, "final", n("59.5"), expected).zeroSuffices(), "Exact minimum");
        var round = GradeCalculator.requiredScore(demo, "final", n("85.001"), expected);
        check(round.displayedRequirement().equals("85.01"), "Threshold rounded upward");
        rejects(() -> GradeCalculator.requiredScore(demo, "final", n("85"), Map.of()), IllegalArgumentException.class, "Missing expectations");
        rejects(() -> GradeCalculator.requiredScore(demo, "final", n("101"), expected), IllegalArgumentException.class, "Invalid target");
        rejects(() -> GradeCalculator.requiredScore(demo, "midterm", n("85"), expected), IllegalArgumentException.class, "Finalized exam cannot be unknown");
        rejects(() -> GradeCalculator.requiredScore(demo, "missing", n("85"), expected), IllegalArgumentException.class, "Missing category");
        rejects(() -> GradeCalculator.requiredScore(new Course("错误权重", List.of(hw)), "homework", n("85"), expected), IllegalArgumentException.class, "Bad weight total");
        rejects(() -> new GradeEntry("a", "a", n("11"), n("10")), IllegalArgumentException.class, "Over maximum");
        rejects(() -> new GradeEntry("a", "a", n("-1"), n("10")), IllegalArgumentException.class, "Negative score");
        rejects(() -> new GradeEntry("a", "a", null, n("0")), IllegalArgumentException.class, "Zero maximum");
        rejects(() -> new GradeEntry("a", "a", null, n("1e-999999")), IllegalArgumentException.class, "Extreme exponent");
        rejects(() -> new GradeCategory("a", "a", n("10"), GradeCategory.Mode.EQUAL, true, hw.entries()), IllegalArgumentException.class, "Incomplete finalized category");
        rejects(() -> new Course("重复", List.of(hw, hw)), IllegalArgumentException.class, "Duplicate categories");
        check(!GradeCalculator.canSolve(hw) && !GradeCalculator.canSolve(empty), "Only a single ungraded exam can be solved");
        var zeroWeight = new GradeCategory("f", "f", n("0"), GradeCategory.Mode.EQUAL, false,
                List.of(new GradeEntry("f", "f", null, n("100"))));
        check(!GradeCalculator.canSolve(zeroWeight), "Zero-weight exam excluded");
        var completed = new GradeCategory("done", "done", n("100"), GradeCategory.Mode.EQUAL, true,
                List.of(new GradeEntry("d", "期末", n("85"), n("100"))));
        close(GradeCalculator.finalizedContribution(new Course("结束", List.of(completed))), "85", "Final total");

        var dir = Files.createTempDirectory("grade-planner-test-");
        try {
            var file = dir.resolve("成绩.xml");
            var special = new Course("中文 & <测试> \"引号\"", List.of(hw, zero));
            CourseStorage.save(special, file);
            check(CourseStorage.load(file).equals(special), "XML round trip Chinese, special chars, blank and zero");
            CourseStorage.save(demo, file);
            check(CourseStorage.load(file).equals(demo), "Atomic overwrite round trip");
            var properties = new Properties();
            try (var in = Files.newInputStream(file)) { properties.loadFromXML(in); }
            properties.setProperty("version", "2");
            try (var out = Files.newOutputStream(file)) { properties.storeToXML(out, "invalid version", "UTF-8"); }
            rejects(() -> CourseStorage.load(file), java.io.IOException.class, "Unsupported file version");
            properties.setProperty("version", "1");
            properties.setProperty("category.0.entry.0.earned", "9999");
            try (var out = Files.newOutputStream(file)) { properties.storeToXML(out, "invalid grade", "UTF-8"); }
            rejects(() -> CourseStorage.load(file), java.io.IOException.class, "Invalid loaded score");
            Files.writeString(file, "not XML");
            rejects(() -> CourseStorage.load(file), java.io.IOException.class, "Corrupt file");
            var missing = dir.resolve("missing-parent").resolve("score.xml");
            rejects(() -> CourseStorage.save(demo, missing), java.io.IOException.class, "Failed save reported");
        } finally {
            try (var files = Files.walk(dir)) {
                for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        System.out.println("PASS: " + checks + " checks (calculations, boundaries, storage)");
    }
}
