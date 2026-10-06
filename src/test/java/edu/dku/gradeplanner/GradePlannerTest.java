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


    // Test-only fixtures are excluded from the application JAR.
    private static GradeEntry entry(String id, String name, String score, String maximum) {
        return new GradeEntry(id, name, score == null ? null : n(score), n(maximum));
    }

    private static Course createFixture() {
        return new Course("Test Course", List.of(
            new GradeCategory("homework", "Homework", n("10"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("hw1", "HW1", "8", "10"), entry("hw2", "HW2", "90", "100"), entry("hw3", "HW3", null, "100"))),
            new GradeCategory("quiz", "Quiz", n("20"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("q1", "Quiz1", "8", "10"), entry("q2", "Quiz2", "9", "10"))),
            new GradeCategory("midterm", "Midterm", n("25"), GradeCategory.Mode.EQUAL, true, List.of(entry("m1", "Midterm", "80", "100"))),
            new GradeCategory("project", "Final project", n("15"), GradeCategory.Mode.EQUAL, false, List.of(entry("p1", "Final project", null, "100"))),
            new GradeCategory("final", "Final exam", n("30"), GradeCategory.Mode.EQUAL, false, List.of(entry("f1", "Final exam", null, "100")))
        ));
    }

    public static void main(String[] args) throws Exception {
        App.configureEnglishLocale();
        check(java.util.Locale.getDefault().equals(java.util.Locale.ENGLISH), "English application locale");
        check(javax.swing.JComponent.getDefaultLocale().equals(java.util.Locale.ENGLISH), "English Swing locale");
        check("Yes".equals(javax.swing.UIManager.getString("OptionPane.yesButtonText")), "English confirmation button");
        check("Open".equals(javax.swing.UIManager.getString("FileChooser.openButtonText")), "English file chooser button");
        check(Course.empty().categories().isEmpty(), "New course contains no seeded categories");
        check(Course.empty().name().equals("Untitled Course"), "Neutral default course name");
        var fixture = createFixture();
        var hw = fixture.categories().getFirst();
        close(GradeCalculator.average(hw).orElseThrow(), "85", "Equal weighting excludes blank");
        var points = new GradeCategory(hw.id(), hw.name(), hw.weight(), GradeCategory.Mode.POINTS, false, hw.entries());
        close(GradeCalculator.average(points).orElseThrow(), "89.0909090909", "Total points weighting");
        var zero = new GradeCategory("z", "Zero", n("10"), GradeCategory.Mode.EQUAL, false, List.of(
                new GradeEntry("z1", "Zero", BigDecimal.ZERO, n("10")),
                new GradeEntry("z2", "Ungraded", null, n("100"))));
        close(GradeCalculator.average(zero).orElseThrow(), "0", "Zero counts; blank does not");
        var empty = new GradeCategory("e", "Empty", n("0"), GradeCategory.Mode.POINTS, false, List.of());
        check(GradeCalculator.average(empty).isEmpty(), "Empty category has no average");
        check(GradeCalculator.average(fixture.categories().get(4)).isEmpty(), "All ungraded has no average");
        close(GradeCalculator.finalizedContribution(fixture), "20", "Only finalized midterm contributes");
        close(GradeCalculator.finalizedAverage(fixture).orElseThrow(), "80", "Finalized average denominator");
        check(GradeCalculator.finalizedAverage(new Course("Empty Course", List.of(empty))).isEmpty(), "No finalized weight");
        var expected = Map.of("homework", n("90"), "quiz", n("85"), "project", n("90"), "midterm", n("0"));
        var r = GradeCalculator.requiredScore(fixture, "final", n("85"), expected);
        close(r.required(), "85", "Target calculation ignores prediction for finalized midterm");
        close(r.minimum(), "59.5", "Minimum total");
        close(r.maximum(), "89.5", "Maximum total");
        check(r.reachable() && !r.zeroSuffices() && r.displayedRequirement().equals("85.00"), "Normal classification");
        check(r.assumptions().size() == 3 && !r.assumptions().containsKey("midterm"), "Assumptions separate from actuals");
        var impossible = GradeCalculator.requiredScore(fixture, "final", n("95"), expected);
        check(!impossible.reachable(), "Impossible target");
        check(GradeCalculator.requiredScore(fixture, "final", n("55"), expected).zeroSuffices(), "Zero suffices");
        check(GradeCalculator.requiredScore(fixture, "final", n("89.5"), expected).required().compareTo(n("100")) == 0, "Exact attainable maximum");
        check(GradeCalculator.requiredScore(fixture, "final", n("59.5"), expected).zeroSuffices(), "Exact minimum");
        var round = GradeCalculator.requiredScore(fixture, "final", n("85.001"), expected);
        check(round.displayedRequirement().equals("85.01"), "Threshold rounded upward");
        rejects(() -> GradeCalculator.requiredScore(fixture, "final", n("85"), Map.of()), IllegalArgumentException.class, "Missing expectations");
        rejects(() -> GradeCalculator.requiredScore(fixture, "final", n("101"), expected), IllegalArgumentException.class, "Invalid target");
        rejects(() -> GradeCalculator.requiredScore(fixture, "midterm", n("85"), expected), IllegalArgumentException.class, "Finalized exam cannot be unknown");
        rejects(() -> GradeCalculator.requiredScore(fixture, "missing", n("85"), expected), IllegalArgumentException.class, "Missing category");
        rejects(() -> GradeCalculator.requiredScore(new Course("Invalid Weights", List.of(hw)), "homework", n("85"), expected), IllegalArgumentException.class, "Bad weight total");
        rejects(() -> new GradeEntry("a", "a", n("11"), n("10")), IllegalArgumentException.class, "Over maximum");
        rejects(() -> new GradeEntry("a", "a", n("-1"), n("10")), IllegalArgumentException.class, "Negative score");
        rejects(() -> new GradeEntry("a", "a", null, n("0")), IllegalArgumentException.class, "Zero maximum");
        rejects(() -> new GradeEntry("a", "a", null, n("1e-999999")), IllegalArgumentException.class, "Extreme exponent");
        rejects(() -> new GradeCategory("a", "a", n("10"), GradeCategory.Mode.EQUAL, true, hw.entries()), IllegalArgumentException.class, "Incomplete finalized category");
        rejects(() -> new Course("Duplicate", List.of(hw, hw)), IllegalArgumentException.class, "Duplicate categories");
        check(!GradeCalculator.canSolve(hw) && !GradeCalculator.canSolve(empty), "Only a single ungraded exam can be solved");
        var zeroWeight = new GradeCategory("f", "f", n("0"), GradeCategory.Mode.EQUAL, false,
                List.of(new GradeEntry("f", "f", null, n("100"))));
        check(!GradeCalculator.canSolve(zeroWeight), "Zero-weight exam excluded");
        var completed = new GradeCategory("done", "done", n("100"), GradeCategory.Mode.EQUAL, true,
                List.of(new GradeEntry("d", "Final Exam", n("85"), n("100"))));
        close(GradeCalculator.finalizedContribution(new Course("Finalized", List.of(completed))), "85", "Final total");

        var dir = Files.createTempDirectory("grade-planner-test-");
        try {
            var file = dir.resolve("\u6210\u7ee9.xml");
            var special = new Course("\u4e2d\u6587 & <\u6d4b\u8bd5> \"\u5f15\u53f7\"", List.of(hw, zero));
            CourseStorage.save(special, file);
            check(CourseStorage.load(file).equals(special), "XML round trip Chinese, special chars, blank and zero");
            CourseStorage.save(fixture, file);
            check(CourseStorage.load(file).equals(fixture), "Atomic overwrite round trip");
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
            rejects(() -> CourseStorage.save(fixture, missing), java.io.IOException.class, "Failed save reported");
        } finally {
            try (var files = Files.walk(dir)) {
                for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        System.out.println("PASS: " + checks + " checks (calculations, boundaries, storage)");
    }
}
