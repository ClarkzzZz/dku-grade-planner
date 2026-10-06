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
    private Course course = Course.empty();
    private Path currentFile;
    private boolean dirty;
    private boolean refreshing;
    private List<GradeCategory> predictionRows = List.of();
    private final JLabel courseTitle = new JLabel("DKU Grade Planner");
    private final JLabel courseName = new JLabel();
    private final JLabel summary = new JLabel();
    private final JLabel categoryDetail = new JLabel();
    private final CategoryTableModel categoryModel = new CategoryTableModel();
    private final EntryTableModel entryModel = new EntryTableModel();
    private final ExpectedTableModel expectedModel = new ExpectedTableModel();
    private final JTable categoryTable = table(categoryModel);
    private final JTable entryTable = table(entryModel);
    private final JTable expectedTable = table(expectedModel);
    private final JComboBox<CategoryChoice> targetExam = new JComboBox<>();
    private final JTextField targetScore = new JTextField(6);
    private final JTextArea result = new JTextArea();
    private final Map<String, String> expectedInputs = new HashMap<>();

    public MainFrame() {
        super("DKU Grade Planner");
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
        var heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.add(courseTitle);
        heading.add(courseName);
        header.add(heading, BorderLayout.NORTH);
        header.add(buttons(button("New", this::newCourse), button("Course Name", this::renameCourse),
                button("Open", this::open),
                button("Save", () -> save(false)), button("Save As", () -> save(true))), BorderLayout.CENTER);
        header.add(summary, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        var categories = new JPanel(new BorderLayout(6, 6));
        categories.setBorder(BorderFactory.createTitledBorder("1 · Categories & Weights"));
        categories.add(new JScrollPane(categoryTable), BorderLayout.CENTER);
        categories.add(buttons(button("Add Category", () -> editCategory(true)),
                button("Edit Category", () -> editCategory(false)), button("Delete Category", this::deleteCategory)), BorderLayout.SOUTH);
        var entries = new JPanel(new BorderLayout(6, 6));
        entries.setBorder(BorderFactory.createTitledBorder("2 · Grade Entries (blank = ungraded; 0 = a real score)"));
        entries.add(categoryDetail, BorderLayout.NORTH);
        entries.add(new JScrollPane(entryTable), BorderLayout.CENTER);
        entries.add(buttons(button("Add Entry", () -> editEntry(true)),
                button("Edit Entry", () -> editEntry(false)), button("Delete Entry", this::deleteEntry)), BorderLayout.SOUTH);
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categories, entries);
        split.setResizeWeight(0.43);
        split.setDividerLocation(490);
        root.add(split, BorderLayout.CENTER);

        var planner = new JPanel(new BorderLayout(8, 8));
        planner.setBorder(BorderFactory.createTitledBorder("3 · Target Calculator (predictions are temporary)"));
        var controls = buttons(new JLabel("Target Grade"), targetScore, new JLabel("Exam to Solve"), targetExam,
                button("Calculate", this::calculate));
        planner.add(controls, BorderLayout.NORTH);
        var assumptions = new JPanel(new BorderLayout(5, 5));
        assumptions.add(new JLabel("Double-click the last column to enter expected final averages (0–100)."), BorderLayout.NORTH);
        assumptions.add(new JScrollPane(expectedTable), BorderLayout.CENTER);
        assumptions.add(buttons(button("Copy Current Averages", this::copyAverages)), BorderLayout.SOUTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setMargin(new Insets(10, 12, 10, 12));
        result.setFont(result.getFont().deriveFont(14f));
        var lower = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, assumptions, new JScrollPane(result));
        lower.setResizeWeight(0.55);
        lower.setDividerLocation(620);
        planner.add(lower, BorderLayout.CENTER);
        planner.add(new JLabel("Standard weighted grading only; no dropped scores, extra credit, curves, or exam pass requirements."), BorderLayout.SOUTH);
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
        refresh(null, null);
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
            courseName.setText("Course: " + course.name());
            setTitle("DKU Grade Planner" + (dirty ? " *" : "") +
                    (currentFile == null ? " · Unsaved" : " · " + currentFile.getFileName()));
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
            String contributionLabel = valid && allFinal ? "Final Grade" : "Finalized Contribution";
            summary.setText("Total Weight: " + display(weight) + "%" + (valid ? "" : " (must total 100%)")
                    + "   |   " + contributionLabel + " " + display(GradeCalculator.finalizedContribution(course))
                    + "   |   Finalized Average: " + GradeCalculator.finalizedAverage(course).map(MainFrame::display).orElse("N/A"));
            if (course.categories().isEmpty())
                summary.setText("No grading categories yet. Add your syllabus categories and weights to get started.");
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
        categoryDetail.setText(c == null ? "Add or select a category." : c.name() + " · " + c.mode()
                + " · Current Average: " + GradeCalculator.average(c).map(MainFrame::display).orElse("N/A")
                + " · " + (c.finalized() ? "Finalized" : "In Progress"));
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
        if (!adding && existing == null) throw new IllegalArgumentException("Select a category first.");
        var name = new JTextField(existing == null ? "" : existing.name(), 20);
        var weight = new JTextField(existing == null ? "0" : existing.weight().toPlainString(), 20);
        var mode = new JComboBox<>(GradeCategory.Mode.values());
        mode.setSelectedItem(existing == null ? GradeCategory.Mode.EQUAL : existing.mode());
        var complete = new JCheckBox("All entries graded; finalize this category", existing != null && existing.finalized());
        var form = form("Name", name, "Weight (%)", weight, "Aggregation", mode, "Status", complete);
        editDialog(adding ? "Add Category" : "Edit Category", form, () -> {
            var edited = new GradeCategory(existing == null ? UUID.randomUUID().toString() : existing.id(), name.getText(),
                    number(weight.getText(), "Weight"), (GradeCategory.Mode) mode.getSelectedItem(), complete.isSelected(),
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
        if (c == null) throw new IllegalArgumentException("Select a category first.");
        if (JOptionPane.showConfirmDialog(this, "Delete " + c.name() + " and all its grade entries?", "Delete Category",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            updateCourse(new Course(course.name(), course.categories().stream().filter(x -> !x.id().equals(c.id())).toList()), null);
    }

    private void editEntry(boolean adding) {
        var c = selectedCategory();
        if (c == null) throw new IllegalArgumentException("Select a category first.");
        int row = entryTable.getSelectedRow();
        if (!adding && row < 0) throw new IllegalArgumentException("Select a grade entry first.");
        var existing = adding ? null : c.entries().get(row);
        var name = new JTextField(existing == null ? "" : existing.name(), 20);
        var earned = new JTextField(existing == null || existing.earned() == null ? "" : existing.earned().toPlainString(), 20);
        var possible = new JTextField(existing == null ? "100" : existing.possible().toPlainString(), 20);
        var form = form("Name", name, "Score (blank if ungraded)", earned, "Maximum Points", possible);
        editDialog(adding ? "Add Entry" : "Edit Entry", form, () -> {
            var edited = new GradeEntry(existing == null ? UUID.randomUUID().toString() : existing.id(), name.getText(),
                    earned.getText().isBlank() ? null : number(earned.getText(), "Score"), number(possible.getText(), "Maximum Points"));
            var entries = new ArrayList<>(c.entries());
            if (adding) entries.add(edited); else entries.set(row, edited);
            replaceCategory(c.withEntries(entries));
        });
    }

    private void deleteEntry() {
        var c = selectedCategory();
        int row = entryTable.getSelectedRow();
        if (c == null || row < 0) throw new IllegalArgumentException("Select a grade entry first.");
        if (JOptionPane.showConfirmDialog(this, "Delete " + c.entries().get(row).name() + "?", "Delete Entry",
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
        editDialog("Course Name", form("Name", field), () -> updateCourse(new Course(field.getText(), course.categories()),
                selectedCategory() == null ? null : selectedCategory().id()));
    }

    private void newCourse() {
        if (!confirmDiscard()) return;
        course = Course.empty();
        currentFile = null;
        dirty = false;
        expectedInputs.clear();
        targetScore.setText("");
        refresh(null, null);
    }

    private JFileChooser chooser() {
        var chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Course Data (*.xml)", "xml"));
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
            targetScore.setText("");
            refresh(null, null);
        } catch (Exception ex) { error("Load failed. Your current data has been kept.\n" + ex.getMessage()); }
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
            if (java.nio.file.Files.exists(path) && JOptionPane.showConfirmDialog(this, "Overwrite the existing file?", "Save",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        }
        try {
            CourseStorage.save(course, path);
            currentFile = path;
            dirty = false;
            setTitle("DKU Grade Planner · " + path.getFileName());
            return true;
        } catch (Exception ex) { error("Save failed: " + ex.getMessage()); return false; }
    }

    private boolean confirmDiscard() {
        if (!dirty) return true;
        int choice = JOptionPane.showConfirmDialog(this, "You have unsaved grade changes. Save them first?", "Unsaved Changes",
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
        if (exam == null) throw new IllegalArgumentException("No eligible exam: use a category with positive weight, exactly one ungraded entry, and an unfinished status.\nIf all categories are finalized, see the final grade at the top.");
        var expected = new HashMap<String, BigDecimal>();
        for (var c : expectedCategories()) expected.put(c.id(), number(expectedInputs.getOrDefault(c.id(), ""), c.name() + " expected final average"));
        var computed = GradeCalculator.requiredScore(course, exam.id(), number(targetScore.getText(), "Target Grade"), expected);
        String headline = !computed.reachable() ? "Target Unreachable (assuming other grades stay as entered)"
                : computed.zeroSuffices() ? "A score of 0 meets the target (assuming other grades stay as entered)"
                : exam.name() + " requires at least " + computed.displayedRequirement() + " / 100";
        var text = new StringBuilder(headline).append("\n\n")
                .append("Projected overall grade with 0 on this exam: ").append(display(computed.minimum()))
                .append("\nProjected overall grade with 100 on this exam: ").append(display(computed.maximum()))
                .append("\n\nAssumptions (expected final category averages):\n");
        for (var c : course.categories()) if (computed.assumptions().containsKey(c.id()))
            text.append(c.name()).append(" = ").append(display(computed.assumptions().get(c.id()))).append("\n");
        if (computed.assumptions().isEmpty()) text.append("None: all other positive-weight categories are finalized.\n");
        text.append("\nFinalized categories use actual grades. This result does not determine whether taking or passing the exam is mandatory.");
        result.setText(text.toString());
        result.setCaretPosition(0);
    }

    private void clearResult() {
        if (course.categories().isEmpty()) {
            result.setText("Get started\n\n1. Set your course name.\n2. Add grading categories and weights from your syllabus.\n3. Add homework, quiz, and exam entries.\n4. Enter your target grade and expected averages.\n\nOr use Open to load a previously saved course.");
            return;
        }
        result.setText("Enter expected grades, then click Calculate.\n\nCurrent averages for unfinished categories reflect graded entries only, not their final contribution.\n\nSolve one unknown exam at a time. Other unfinished categories require explicit assumptions.");
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Operation failed." : message, "Check Your Input", JOptionPane.ERROR_MESSAGE);
    }

    private static BigDecimal number(String text, String label) {
        try {
            var trimmed = text.strip();
            if (trimmed.isEmpty()) throw new IllegalArgumentException("Please enter " + label + ".");
            if (trimmed.length() > 80) throw new NumberFormatException();
            var value = new BigDecimal(trimmed);
            if (value.precision() > 40 || Math.abs((long) value.scale()) > 60) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " must be a valid number (use a decimal point)."); }
    }

    private static String display(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private record CategoryChoice(String id, String name) { @Override public String toString() { return name; } }

    private final class CategoryTableModel extends AbstractTableModel {
        private final String[] columns = {"Category", "Weight %", "Current Avg.", "Status"};
        public int getRowCount() { return course.categories().size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public Object getValueAt(int row, int col) {
            var c = course.categories().get(row);
            return switch (col) {
                case 0 -> c.name();
                case 1 -> c.weight().stripTrailingZeros().toPlainString();
                case 2 -> GradeCalculator.average(c).map(MainFrame::display).orElse("N/A");
                default -> c.finalized() ? "Finalized" : "In Progress";
            };
        }
    }

    private final class EntryTableModel extends AbstractTableModel {
        private final String[] columns = {"Name", "Score", "Maximum Points", "Percent", "Status"};
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
                default -> e.earned() == null ? "Ungraded" : "Graded";
            };
        }
    }

    private final class ExpectedTableModel extends AbstractTableModel {
        private final String[] columns = {"Unfinished Category", "Current Avg. (Reference)", "Expected Final Avg."};
        public int getRowCount() { return predictionRows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int column) { return columns[column]; }
        public boolean isCellEditable(int row, int col) { return col == 2; }
        public Object getValueAt(int row, int col) {
            var c = predictionRows.get(row);
            return switch (col) {
                case 0 -> c.name();
                case 1 -> GradeCalculator.average(c).map(MainFrame::display).orElse("N/A");
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
