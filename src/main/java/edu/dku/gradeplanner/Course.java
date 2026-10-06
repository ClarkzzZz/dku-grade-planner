package edu.dku.gradeplanner;

import java.util.HashSet;
import java.util.List;

public record Course(String name, List<GradeCategory> categories) {
    public Course {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("课程名称不能为空。");
        name = name.strip();
        categories = List.copyOf(categories);
        if (categories.size() > 100) throw new IllegalArgumentException("每门课程最多 100 个类别。");
        var ids = new HashSet<String>();
        var names = new HashSet<String>();
        for (var category : categories) {
            if (!ids.add(category.id())) throw new IllegalArgumentException("类别 ID 重复。");
            if (!names.add(category.name())) throw new IllegalArgumentException("类别名称不能重复。");
        }
    }
}
