package edu.dku.gradeplanner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Swing controller. Model and calculator contain no UI dependencies. */
public final class MainFrame extends JFrame {
    private Course course = DemoData.create();
    private Path currentFile;
    private boolean dirty;
    private boolean refreshing;
    private List<GradeCategory> predictionRows = List.of();
    private final JLabel courseTitle = new JLabel();
    private final JLabel summary = new JLabel();
    private final JLabel categoryDetail = new JLabel();
    private final CategoryTableModel categoryModel = new CategoryTableModel();
    private final EntryTableModel entryModel = new EntryTableModel();
    private final ExpectedTableModel expectedModel = new ExpectedTableModel();
    private final JTable categoryTable = table(categoryModel);
    private final JTable entryTable = table(entryModel);
    private final JTable expectedTable = table(expectedModel);
    private final JComboBox<CategoryChoice> targetExam = new JComboBox<>();
    private final JTextField targetScore = new JTextField("85", 6);
    private final JTextArea result = new JTextArea();
    private final Map<String, String> expectedInputs = new HashMap<>();

    public MainFrame() {
        super("DKU Grade Planner · Java 21");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (confirmDiscard()) dispose();
            }
        });
        var root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(root);
        courseTitle.setFont(courseTitle.getFont().deriveFont(Font.BOLD, 21f));
        var header = new JPanel(new BorderLayout(6, 6));
        header.add(courseTitle, BorderLayout.NORTH);
        header.add(buttons(button("新建", this::newCourse), button("课程名称", this::renameCourse),
                button("加载示例", this::loadDemo), button("打开", this::open),
                button("保存", () -> save(false)), button("另存为", () -> save(true))), BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        var categories = new JPanel(new BorderLayout(6, 6));
        categories.setBorder(BorderFactory.createTitledBorder("1 · 考核类别与权重"));
        categories.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        categories.add(buttons(button("添加类别", () -> editCategory(true)),
                button("编辑类别", () -> editCategory(false)), button("删除类别", this::deleteCategory)), BorderLayout.SOUTH);
        var entries = new JPanel(new BorderLayout(6, 6));
        entries.setBorder(BorderFactory.createTitledBorder("2 · 多次成绩录入（空得分 = 未评分，0 = 零分）"));
        entries.add(categoryDetail, BorderLayout.NORTH);
        entries.add(new JScrollPane(entryTable), BorderLayout.CENTER);
        entries.add(buttons(button("添加成绩", () -> editEntry(true)),
                button("编辑成绩", () -> editEntry(false)), button("删除成绩", this::deleteEntry)), BorderLayout.SOUTH);
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categories, entries);
        split.setResizeWeight(0.43);
        split.setDividerLocation(490);
        root.add(split, BorderLayout.CENTER);

        var planner = new JPanel(new BorderLayout(8, 8));
        planner.setBorder(BorderFactory.createTitledBorder("3 · 目标反推（预计值不会覆盖真实成绩，也不保存到成绩文件）"));
        var controls = buttons(new JLabel("目标总分"), targetScore, new JLabel("待反推考试"), targetExam,
                button("计算", this::calculate));
        planner.add(controls, BorderLayout.NORTH);
        var assumptions = new JPanel(new BorderLayout(5, 5));
        assumptions.add(new JLabel("双击最后一列输入其他类别的预计最终平均分（0–100）"), BorderLayout.NORTH);
        assumptions.add(new JScrollPane(expectedTable), BorderLayout.CENTER);
        assumptions.add(buttons(button("将当前平均复制为预测", this::copyAverages)), BorderLayout.SOUTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setMargin(new Insets(10, 12, 10, 12));
        result.setFont(result.getFont().deriveFont(14f));
        var lower = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, assumptions, new JScrollPane(result));
        lower.setResizeWeight(0.55);
        lower.setDividerLocation(620);
        planner.add(lower, BorderLayout.CENTER);
        planner.add(new JLabel("仅支持普通加权评分；不自动处理最低分剔除、额外加分、曲线或必须通过考试的规则。"), BorderLayout.SOUTH);
        planner.setPreferredSize(new Dimension(1100, 310));
        root.add(planner, BorderLayout.SOUTH);

        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshEntries();
        });
        targetExam.addActionListener(e -> {
            if (!refreshing) {
                stopPredictionEditing();
                refreshPredictions();
                clearResult();
            }
        });
        targetScore.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearResult(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearResult(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { clearResult(); }
        });
        setMinimumSize(new Dimension(1050, 760));
        setSize(1240, 880);
        setLocationRelativeTo(null);
        refresh(null, "final");
    }

    private static JTable table(AbstractTableModel model) {
        var table = new JTable(model);
        table.setRowHeight(29);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private JButton button(String label, Runnable action) {
        var button = new JButton(label);
        button.addActionListener(e -> {
            try { stopPredictionEditing(); action.run(); }
            catch (Exception ex) { error(ex.getMessage()); }
        });
        return button;
    }

    private static JPanel buttons(Component... components) {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        for (var component : components) panel.add(component);
        return panel;
    }

    private GradeCategory selectedCategory() {
        int index = categoryTable.getSelectedRow();
        return index < 0 || index >= course.categories().size() ? null : course.categories().get(index);
    }

    private void refresh(String selectedId, String preferredTargetId) {
        stopPredictionEditing();
        refreshing = true;
        try {
            courseTitle.setText(course.name());
            setTitle("DKU Grade Planner" + (dirty ? " *" : "") +
                    (currentFile == null ? " · 未保存" : " · " + currentFile.getFileName()));
            categoryModel.fireTableDataChanged();
            if (!course.categories().isEmpty()) {
                int row = 0;
                for (int i = 0; i < course.categories().size(); i++)
                    if (course.categories().get(i).id().equals(selectedId)) row = i;
                categoryTable.setRowSelectionInterval(row, row);
            }
            refreshEntries();
            var weight = GradeCalculator.totalWeight(course);
            boolean valid = weight.compareTo(new BigDecimal("100")) == 0;
            var allFinal = !course.categories().isEmpty() && course.categories().stream().allMatch(GradeCategory::finalized);
            String contributionLabel = valid && allFinal ? "最终总分" : "已结束类别贡献";
            summary.setText("权重合计 " + display(weight) + "%" + (valid ? "" : "（需调整为100%）")
                    + "   |   " + contributionLabel + " " + display(GradeCalculator.finalizedContribution(course))
                    + "   |   已结束类别平均 " + GradeCalculator.finalizedAverage(course).map(MainFrame::display).orElse("暂无"));
            targetExam.removeAllItems();
            for (var c : course.categories()) if (GradeCalculator.canSolve(c)) targetExam.addItem(new CategoryChoice(c.id(), c.name()));
            for (int i = 0; i < targetExam.getItemCount(); i++)
                if (targetExam.getItemAt(i).id().equals(preferredTargetId)) targetExam.setSelectedIndex(i);
            expectedInputs.keySet().removeIf(id -> course.categories().stream().noneMatch(c -> c.id().equals(id)));
            refreshPredictions();
            clearResult();
        } finally { refreshing = false; }
    }

    private void refreshEntries() {
        entryModel.fireTableDataChanged();
        var c = selectedCategory();
        categoryDetail.setText(c == null ? "请添加或选择一个类别。" : c.name() + " · " + c.mode()
                + " · 当前平均 " + GradeCalculator.average(c).map(MainFrame::display).orElse("暂无")
                + " · " + (c.finalized() ? "已结束" : "尚未结束"));
    }

    private void updateCourse(Course updated, String selectedId) {
        var target = (CategoryChoice) targetExam.getSelectedItem();
        course = updated;
        dirty = true;
        refresh(selectedId, target == null ? null : target.id());
    }

    private void replaceCategory(GradeCategory updated) {
        var categories = new ArrayList<>(course.categories());
        for (int i = 0; i < categories.size(); i++)
            if (categories.get(i).id().equals(updated.id())) categories.set(i, updated);
        updateCourse(new Course(course.name(), categories), updated.id());
    }

    private void editCategory(boolean adding) {
        var existing = adding ? null : selectedCategory();
        if (!adding && existing == null) throw new IllegalArgumentException("请先选择类别。");
        var name = new JTextField(existing == null ? "" : existing.name(), 20);
        var weight = new JTextField(existing == null ? "0" : existing.weight().toPlainString(), 20);
        var mode = new JComboBox<>(GradeCategory.Mode.values());
        mode.setSelectedItem(existing == null ? GradeCategory.Mode.EQUAL : existing.mode());
        var complete = new JCheckBox("全部条目已评分，类别已结束", existing != null && existing.finalized());
        var form = form("名称", name, "权重（%）", weight, "汇总方式", mode, "状态", complete);
        editDialog(adding ? "添加类别" : "编辑类别", form, () -> {
            var edited = new GradeCategory(existing == null ? UUID.randomUUID().toString() : existing.id(), name.getText(),
                    number(weight.getText(), "权重"), (GradeCategory.Mode) mode.getSelectedItem(), complete.isSelected(),
                    existing == null ? List.of() : existing.entries());
            if (adding) {
                var categories = new ArrayList<>(course.categories());
                categories.add(edited);
                updateCourse(new Course(course.name(), categories), edited.id());
            } else replaceCategory(edited);
        });
    }

    private void deleteCategory() {
        var c = selectedCategory();
        if (c == null) throw new IllegalArgumentException("请先选择类别。");
        if (JOptionPane.showConfirmDialog(this, "删除 " + c.name() + " 及其中全部成绩？", "删除类别",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            updateCourse(new Course(course.name(), course.categories().stream().filter(x -> !x.id().equals(c.id())).toList()), null);
    }

    private void editEntry(boolean adding) {
        var c = selectedCategory();
        if (c == null) throw new IllegalArgumentException("请先选择类别。");
        int row = entryTable.getSelectedRow();
        if (!adding && row < 0) throw new IllegalArgumentException("请先选择成绩条目。");
        var existing = adding ? null : c.entries().get(row);
        var name = new JTextField(existing == null ? "" : existing.name(), 20);
        var earned = new JTextField(existing == null || existing.earned() == null ? "" : existing.earned().toPlainString(), 20);
        var possible = new JTextField(existing == null ? "100" : existing.possible().toPlainString(), 20);
        var form = form("名称", name, "得分（未评分留空）", earned, "满分", possible);
        editDialog(adding ? "添加成绩" : "编辑成绩", form, () -> {
            var edited = new GradeEntry(existing == null ? UUID.randomUUID().toString() : existing.id(), name.getText(),
                    earned.getText().isBlank() ? null : number(earned.getText(), "得分"), number(possible.getText(), "满分"));
            var entries = new ArrayList<>(c.entries());
            if (adding) entries.add(edited); else entries.set(row, edited);
            replaceCategory(c.withEntries(entries));
        });
    }

    private void deleteEntry() {
        var c = selectedCategory();
        int row = entryTable.getSelectedRow();
        if (c == null || row < 0) throw new IllegalArgumentException("请先选择成绩条目。");
        if (JOptionPane.showConfirmDialog(this, "删除 " + c.entries().get(row).name() + "？", "删除成绩",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        var entries = new ArrayList<>(c.entries());
        entries.remove(row);
        replaceCategory(c.withEntries(entries));
    }

    /** Keep the form open after invalid input so users can correct it. */
    private void editDialog(String title, JPanel form, Runnable commit) {
        while (JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try { commit.run(); return; }
            catch (IllegalArgumentException ex) { error(ex.getMessage()); }
        }
    }

    private static JPanel form(Object... fields) {
        var form = new JPanel(new GridLayout(fields.length / 2, 2, 10, 12));
        for (int i = 0; i < fields.length; i += 2) {
            form.add(new JLabel((String) fields[i]));
            form.add((Component) fields[i + 1]);
        }
        return form;
    }

    private void renameCourse() {
        var field = new JTextField(course.name(), 25);
        editDialog("课程名称", form("名称", field), () -> updateCourse(new Course(field.getText(), course.categories()),
                selectedCategory() == null ? null : selectedCategory().id()));
    }

    private void newCourse() {
        if (!confirmDiscard()) return;
        course = new Course("我的课程", List.of());
        currentFile = null;
        dirty = false;
        expectedInputs.clear();
        refresh(null, null);
    }

    private void loadDemo() {
        if (!confirmDiscard()) return;
        course = DemoData.create();
        currentFile = null;
        dirty = false;
        expectedInputs.clear();
        refresh(null, "final");
    }

    private JFileChooser chooser() {
        var chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("课程数据 (*.xml)", "xml"));
        if (currentFile != null) chooser.setSelectedFile(currentFile.toFile());
        return chooser;
    }

    private void open() {
        if (!confirmDiscard()) return;
        var chooser = chooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        var path = chooser.getSelectedFile().toPath();
        try {
            var loaded = CourseStorage.load(path);
            course = loaded;
            currentFile = path;
            dirty = false;
            expectedInputs.clear();
            refresh(null, "final");
        } catch (Exception ex) { error("加载失败，当前数据保留。\n" + ex.getMessage()); }
    }

    private boolean save(boolean saveAs) {
        Path path = currentFile;
        if (path == null || saveAs) {
            var chooser = chooser();
            if (path == null) chooser.setSelectedFile(new java.io.File("course-grades.xml"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
            path = chooser.getSelectedFile().toPath();
            if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xml"))
                path = path.resolveSibling(path.getFileName() + ".xml");
            if (java.nio.file.Files.exists(path) && JOptionPane.showConfirmDialog(this, "覆盖已有文件？", "保存",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        }
        try {
            CourseStorage.save(course, path);
            currentFile = path;
            dirty = false;
            setTitle("DKU Grade Planner · " + path.getFileName());
            return true;
        } catch (Exception ex) { error("保存失败：" + ex.getMessage()); return false; }
    }

    private boolean confirmDiscard() {
        if (!dirty) return true;
        int choice = JOptionPane.showConfirmDialog(this, "有未保存的成绩修改。先保存吗？", "未保存修改",
                JOptionPane.YES_NO_CANCEL_OPTION);
        return choice == JOptionPane.NO_OPTION || (choice == JOptionPane.YES_OPTION && save(false));
    }

    private void stopPredictionEditing() {
        if (expectedTable.isEditing()) expectedTable.getCellEditor().stopCellEditing();
    }

    private void refreshPredictions() {
        predictionRows = expectedCategories();
        expectedModel.fireTableDataChanged();
    }

    private void copyAverages() {
        stopPredictionEditing();
        for (var c : expectedCategories())
            GradeCalculator.average(c).ifPresent(value -> expectedInputs.put(c.id(), value.stripTrailingZeros().toPlainString()));
        expectedModel.fireTableDataChanged();
        clearResult();
    }

    private List<GradeCategory> expectedCategories() {
        var chosen = (CategoryChoice) targetExam.getSelectedItem();
        return course.categories().stream().filter(c -> !c.finalized() && c.weight().signum() > 0
                && (chosen == null || !c.id().equals(chosen.id()))).toList();
    }

    private void calculate() {
        stopPredictionEditing();
        var exam = (CategoryChoice) targetExam.getSelectedItem();
        if (exam == null) throw new IllegalArgumentException("暂无可反推考试：需要正权重、仅一条未评分成绩，且类别未结束。\n如全部类别已结束，请查看顶部最终总分。");
        var expected = new HashMap<String, BigDecimal>();
        for (var c : expectedCategories()) expected.put(c.id(), number(expectedInputs.getOrDefault(c.id(), ""), c.name() + " 预计成绩"));
        var computed = GradeCalculator.requiredScore(course, exam.id(), number(targetScore.getText(), "目标总分"), expected);
        String headline = !computed.reachable() ? "目标无法达到（其他成绩假设不变）"
                : computed.zeroSuffices() ? "该考试 0 分也能达到目标（其他成绩假设不变）"
                : exam.name() + " 至少需要 " + computed.displayedRequirement() + " / 100";
        var text = new StringBuilder(headline).append("\n\n")
                .append("该考试 0 分时预计总分：").append(display(computed.minimum()))
                .append("\n该考试 100 分时预计总分：").append(display(computed.maximum()))
                .append("\n\n本次假设（预计最终类别平均分）：\n");
        for (var c : course.categories()) if (computed.assumptions().containsKey(c.id()))
            text.append(c.name()).append(" = ").append(display(computed.assumptions().get(c.id()))).append("\n");
        if (computed.assumptions().isEmpty()) text.append("无：其他正权重类别均已结束。\n");
        text.append("\n已结束类别使用实际成绩。此结果不判断是否必须参加或通过考试。");
        result.setText(text.toString());
        result.setCaretPosition(0);
    }

    private void clearResult() {
        result.setText("填写预测值并点击“计算”。\n\n未结束类别的当前平均只代表已有成绩，不能直接视为最终贡献。\n\n反推支持一项未知考试；多个未结束类别需要明确假设。");
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "操作失败。" : message, "请检查输入", JOptionPane.ERROR_MESSAGE);
    }

    private static BigDecimal number(String text, String label) {
        try {
            var trimmed = text.strip();
            if (trimmed.isEmpty()) throw new IllegalArgumentException("请填写 " + label + "。");
            if (trimmed.length() > 80) throw new NumberFormatException();
            var value = new BigDecimal(trimmed);
            if (value.precision() > 40 || Math.abs((long) value.scale()) > 60) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " 必须是有效数字（使用英文小数点）。"); }
    }

    private static String display(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private record CategoryChoice(String id, String name) { @Override public String toString() { return name; } }

    private final class CategoryTableModel extends AbstractTableModel {
        private final String[] columns = {"类别", "权重 %", "当前平均", "状态"};
        public int getRowCount() { return course.categories().size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int col) {
            var c = course.categories().get(row);
            return switch (col) {
                case 0 -> c.name();
                case 1 -> c.weight().stripTrailingZeros().toPlainString();
                case 2 -> GradeCalculator.average(c).map(MainFrame::display).orElse("暂无");
                default -> c.finalized() ? "已结束" : "未结束";
            };
        }
    }

    private final class EntryTableModel extends AbstractTableModel {
        private final String[] columns = {"名称", "得分", "满分", "百分制", "状态"};
        public int getRowCount() { var c = selectedCategory(); return c == null ? 0 : c.entries().size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int col) {
            var e = selectedCategory().entries().get(row);
            return switch (col) {
                case 0 -> e.name();
                case 1 -> e.earned() == null ? "—" : e.earned().stripTrailingZeros().toPlainString();
                case 2 -> e.possible().stripTrailingZeros().toPlainString();
                case 3 -> e.earned() == null ? "—" : display(e.earned().multiply(new BigDecimal("100")).divide(e.possible(), java.math.MathContext.DECIMAL128));
                default -> e.earned() == null ? "未评分" : "已评分";
            };
        }
    }

    private final class ExpectedTableModel extends AbstractTableModel {
        private final String[] columns = {"未结束类别", "当前平均（参考）", "预计最终平均（输入）"};
        public int getRowCount() { return predictionRows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public boolean isCellEditable(int row, int col) { return col == 2; }
        public Object getValueAt(int row, int col) {
            var c = predictionRows.get(row);
            return switch (col) {
                case 0 -> c.name();
                case 1 -> GradeCalculator.average(c).map(MainFrame::display).orElse("暂无");
                default -> expectedInputs.getOrDefault(c.id(), "");
            };
        }
        public void setValueAt(Object value, int row, int col) {
            expectedInputs.put(predictionRows.get(row).id(), String.valueOf(value).strip());
            fireTableCellUpdated(row, col);
            clearResult();
        }
    }
}
