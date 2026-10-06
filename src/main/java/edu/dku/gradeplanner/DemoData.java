package edu.dku.gradeplanner;

import java.math.BigDecimal;
import java.util.List;

public final class DemoData {
    private DemoData() { }
    private static BigDecimal n(String value) { return new BigDecimal(value); }
    private static GradeEntry entry(String id, String name, String score, String maximum) {
        return new GradeEntry(id, name, score == null ? null : n(score), n(maximum));
    }

    public static Course create() {
        return new Course("DKU Sample Course (Demo Data)", List.of(
            new GradeCategory("homework", "Homework", n("10"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("hw1", "HW1", "8", "10"), entry("hw2", "HW2", "90", "100"), entry("hw3", "HW3", null, "100"))),
            new GradeCategory("quiz", "Quiz", n("20"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("q1", "Quiz1", "8", "10"), entry("q2", "Quiz2", "9", "10"))),
            new GradeCategory("midterm", "Midterm", n("25"), GradeCategory.Mode.EQUAL, true, List.of(
                entry("m1", "Midterm", "80", "100"))),
            new GradeCategory("project", "Final project", n("15"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("p1", "Final project", null, "100"))),
            new GradeCategory("final", "Final exam", n("30"), GradeCategory.Mode.EQUAL, false, List.of(
                entry("f1", "Final exam", null, "100")))
        ));
    }

    public static void main(String[] args) throws java.io.IOException {
        if (args.length != 1) throw new IllegalArgumentException("Specify output XML path.");
        CourseStorage.save(create(), java.nio.file.Path.of(args[0]));
    }
}
