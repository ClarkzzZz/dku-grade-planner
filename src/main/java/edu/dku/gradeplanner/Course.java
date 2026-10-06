package edu.dku.gradeplanner;

import java.util.HashSet;
import java.util.List;

public record Course(String name, List<GradeCategory> categories) {
    public Course {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Course name cannot be blank.");
        name = name.strip();
        categories = List.copyOf(categories);
        if (categories.size() > 100) throw new IllegalArgumentException("A course can have at most 100 categories.");
        var ids = new HashSet<String>();
        var names = new HashSet<String>();
        for (var category : categories) {
            if (!ids.add(category.id())) throw new IllegalArgumentException("Duplicate category ID.");
            if (!names.add(category.name())) throw new IllegalArgumentException("Category names must be unique.");
        }
    }
}
