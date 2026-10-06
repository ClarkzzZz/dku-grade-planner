package edu.dku.gradeplanner;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Properties;

/** Versioned UTF-8 XML using only the JDK. Scores and display order round-trip exactly. */
public final class CourseStorage {
    private CourseStorage() { }

    public static void save(Course course, Path destination) throws IOException {
        var p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("course.name", course.name());
        p.setProperty("category.count", String.valueOf(course.categories().size()));
        for (int i = 0; i < course.categories().size(); i++) {
            var c = course.categories().get(i);
            var prefix = "category." + i + ".";
            p.setProperty(prefix + "id", c.id());
            p.setProperty(prefix + "name", c.name());
            p.setProperty(prefix + "weight", c.weight().toPlainString());
            p.setProperty(prefix + "mode", c.mode().name());
            p.setProperty(prefix + "finalized", String.valueOf(c.finalized()));
            p.setProperty(prefix + "entry.count", String.valueOf(c.entries().size()));
            for (int j = 0; j < c.entries().size(); j++) {
                var e = c.entries().get(j);
                var key = prefix + "entry." + j + ".";
                p.setProperty(key + "id", e.id());
                p.setProperty(key + "name", e.name());
                p.setProperty(key + "possible", e.possible().toPlainString());
                p.setProperty(key + "earned", e.earned() == null ? "" : e.earned().toPlainString());
            }
        }
        var target = destination.toAbsolutePath();
        var temp = Files.createTempFile(target.getParent(), ".grade-planner-", ".tmp");
        try {
            try (var out = Files.newOutputStream(temp)) {
                p.storeToXML(out, "DKU Grade Planner — real scores only; predictions are not saved", "UTF-8");
            }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public static Course load(Path path) throws IOException {
        if (Files.size(path) > 2_000_000) throw new IOException("The course file exceeds the 2 MB limit.");
        var p = new Properties();
        try (var in = Files.newInputStream(path)) { p.loadFromXML(in); }
        try {
            if (!"1".equals(required(p, "version"))) throw new IllegalArgumentException("Unsupported course file version.");
            var categories = new ArrayList<GradeCategory>();
            int count = count(p, "category.count", 100);
            for (int i = 0; i < count; i++) {
                var prefix = "category." + i + ".";
                var entries = new ArrayList<GradeEntry>();
                int entryCount = count(p, prefix + "entry.count", 1000);
                for (int j = 0; j < entryCount; j++) {
                    var key = prefix + "entry." + j + ".";
                    var score = required(p, key + "earned");
                    entries.add(new GradeEntry(required(p, key + "id"), required(p, key + "name"),
                            score.isBlank() ? null : new BigDecimal(score), new BigDecimal(required(p, key + "possible"))));
                }
                var finalized = required(p, prefix + "finalized");
                if (!finalized.equals("true") && !finalized.equals("false"))
                    throw new IllegalArgumentException("Invalid category completion status.");
                categories.add(new GradeCategory(required(p, prefix + "id"), required(p, prefix + "name"),
                        new BigDecimal(required(p, prefix + "weight")),
                        GradeCategory.Mode.valueOf(required(p, prefix + "mode")), Boolean.parseBoolean(finalized), entries));
            }
            return new Course(required(p, "course.name"), categories);
        } catch (IllegalArgumentException e) { throw new IOException("Invalid course file: " + e.getMessage(), e); }
    }

    private static String required(Properties p, String key) {
        var value = p.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key);
        return value;
    }

    private static int count(Properties p, String key, int limit) {
        int value = Integer.parseInt(required(p, key));
        if (value < 0 || value > limit) throw new IllegalArgumentException("Item count exceeds the limit: " + key);
        return value;
    }
}
