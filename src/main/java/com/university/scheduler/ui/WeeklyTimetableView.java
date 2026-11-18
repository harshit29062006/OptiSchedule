package com.university.scheduler.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Weekly timetable generated from the original university workbook.
 * The CSV contains one row per populated timetable cell, preserving its
 * programme, weekday, period, class group, room/facility, and class type(s).
 */
public final class WeeklyTimetableView {
    private record Entry(
            String sheet,
            String day,
            int period,
            String time,
            String groups,
            String course,
            String room,
            String faculty,
            String sourceCell,
            String classTypes,
            int durationPeriods
    ) {
        List<String> groupList() {
            if (groups == null || groups.isBlank()) return List.of();
            return Arrays.stream(groups.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }

        boolean hasGroup(String group) {
            return groupList().contains(group);
        }

        boolean hasType(String type) {
            if (classTypes == null || classTypes.isBlank()) return false;
            return Arrays.asList(classTypes.split("\\|")).contains(type);
        }

        boolean isCombined() {
            return classTypes != null && classTypes.contains("|");
        }

        String searchable() {
            return (sheet + " " + groups + " " + course + " " + room + " " + faculty + " " + classTypes)
                    .toLowerCase(Locale.ROOT);
        }
    }

    private static final String ALL_PROGRAMMES = "All programmes";
    private static final String ALL_SECTIONS = "All sections";

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    // The period starts are taken from the HOURS column of the source workbook.
    private static final List<String> TIMES = List.of(
            "08:00", "08:50", "09:40", "10:30", "11:20", "12:10", "13:00",
            "13:50", "14:40", "15:30", "16:20", "17:10", "18:00", "18:50");

    private static final List<String> PROGRAMME_ORDER = List.of(
            "FIRST YEAR A", "FIRST YEAR B",
            "2ND YEAR A", "2ND YEARB B",
            "3RD YEAR A", "3RD YEAR B",
            "4TH YEAR A", "4TH YEAR B",
            "PG TIME TABLE");

    private final List<Entry> all = new ArrayList<>();
    private final ComboBox<String> sheet = new ComboBox<>();
    private final ComboBox<String> group = new ComboBox<>();
    private final TextField search = new TextField();
    private final TableView<String[]> table = new TableView<>();
    private final ObservableList<String[]> visible = FXCollections.observableArrayList();
    private final Label status = new Label();

    public Parent getView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(14));

        Label intro = new Label(
                "Official July–December 2026 timetable. Choose All programmes to view every programme, " +
                "or select one programme for a clearer individual timetable. The grid includes lectures, " +
                "tutorials and practicals; multi-period labs are shown at their correct start time and duration.");
        intro.setWrapText(true);

        sheet.setPromptText("Programme");
        sheet.setPrefWidth(205);

        group.setPromptText("Section / subgroup");
        group.setPrefWidth(175);

        search.setPromptText("Search course or group");
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox filters = new HBox(9,
                new Label("Programme:"), sheet,
                new Label("Section:"), group,
                search);
        filters.setAlignment(Pos.CENTER_LEFT);

        table.getColumns().add(column("Period / Time", 0, 125));
        for (int i = 0; i < DAYS.size(); i++) {
            table.getColumns().add(column(DAYS.get(i), i + 1, 165));
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(-1);
        table.setItems(visible);
        table.setPlaceholder(new Label("No classes match these filters."));
        VBox.setVgrow(table, Priority.ALWAYS);

        Label legend = new Label(
                "L = Lecture   ·   T = Tutorial   ·   P = Practical / Lab   ·   Combined cells retain every class type shown in Excel");
        legend.setWrapText(true);
        legend.getStyleClass().add("muted-label");

        Button apply = new Button("Apply filters");
        apply.getStyleClass().add("primary-button");
        apply.setOnAction(e -> refresh());

        sheet.setOnAction(e -> loadGroups());
        group.setOnAction(e -> refresh());
        search.setOnAction(e -> refresh());

        root.getChildren().addAll(intro, filters, apply, legend, table, status);
        loadData();
        return root;
    }

    private TableColumn<String[], String> column(String title, int index, double width) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new SimpleStringProperty(v.getValue()[index]));
        c.setPrefWidth(width);
        c.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMinHeight(Region.USE_PREF_SIZE);
                label.setPadding(new Insets(6));
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setGraphic(null);
                } else {
                    label.setText(value);
                    setGraphic(label);
                }
            }
        });
        return c;
    }

    // CSV parser supporting commas, escaped quotes, and quoted fields.
    // The generator flattens embedded Excel line breaks so each record stays on one CSV line.
    private List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(value.toString());
                value.setLength(0);
            } else {
                value.append(ch);
            }
        }
        fields.add(value.toString());
        return fields;
    }

    private void loadData() {
        try (InputStream in = getClass().getResourceAsStream("/data/weekly_timetable.csv")) {
            if (in == null) {
                throw new IllegalStateException("Missing /data/weekly_timetable.csv in application resources.");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                br.readLine(); // header
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    List<String> x = parseCsv(line);
                    if (x.size() < 11) continue;

                    try {
                        all.add(new Entry(
                                clean(x.get(0)),
                                normalizeDay(clean(x.get(1))),
                                Integer.parseInt(clean(x.get(2))),
                                clean(x.get(3)),
                                clean(x.get(4)),
                                clean(x.get(5)),
                                clean(x.get(6)),
                                clean(x.get(7)),
                                clean(x.get(8)),
                                clean(x.get(9)),
                                Math.max(1, Integer.parseInt(clean(x.get(10))))
                        ));
                    } catch (NumberFormatException ignored) {
                        // Ignore a malformed row, but keep loading all valid source rows.
                    }
                }
            }

            List<String> programmes = all.stream()
                    .map(Entry::sheet)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted(Comparator
                            .comparingInt((String s) -> programmeOrder(s))
                            .thenComparing(String.CASE_INSENSITIVE_ORDER))
                    .toList();

            List<String> options = new ArrayList<>();
            options.add(ALL_PROGRAMMES);
            options.addAll(programmes);
            sheet.setItems(FXCollections.observableArrayList(options));
            sheet.getSelectionModel().select(ALL_PROGRAMMES);
            loadGroups();
        } catch (Exception ex) {
            UiSupport.error("Could not load the official weekly timetable", ex);
        }
    }

    private int programmeOrder(String value) {
        int index = PROGRAMME_ORDER.indexOf(value.trim().toUpperCase(Locale.ROOT));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private String clean(String s) {
        return s == null ? "" : s.replace("\uFEFF", "").trim();
    }

    private String normalizeDay(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private void loadGroups() {
        boolean allProgrammes = ALL_PROGRAMMES.equals(sheet.getValue());
        group.setDisable(allProgrammes);

        List<String> options = new ArrayList<>();
        options.add(ALL_SECTIONS);

        if (!allProgrammes) {
            String selectedSheet = sheet.getValue();
            all.stream()
                    .filter(e -> Objects.equals(e.sheet(), selectedSheet))
                    .flatMap(e -> e.groupList().stream())
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(options::add);
        }

        group.setItems(FXCollections.observableArrayList(options));
        group.getSelectionModel().selectFirst();
        refresh();
    }

    private void refresh() {
        String selectedSheet = sheet.getValue();
        String selectedGroup = group.getValue();
        String q = search.getText() == null
                ? ""
                : search.getText().trim().toLowerCase(Locale.ROOT);

        boolean allProgrammes = ALL_PROGRAMMES.equals(selectedSheet);
        boolean allSections = selectedGroup == null
                || selectedGroup.isBlank()
                || ALL_SECTIONS.equals(selectedGroup);

        List<Entry> sheetEntries = all.stream()
                .filter(e -> allProgrammes || Objects.equals(e.sheet(), selectedSheet))
                .toList();

        List<Entry> found = sheetEntries.stream()
                .filter(e -> allSections || matchesSelectedGroup(e, selectedGroup, selectedSheet, sheetEntries))
                .filter(e -> q.isBlank() || e.searchable().contains(q))
                .toList();

        // All programme sheets use the same period start times. The selected sheet's
        // own values are used when one programme is selected; otherwise all source rows vote.
        Map<Integer, Map<String, Integer>> timeCounts = new HashMap<>();
        for (Entry e : sheetEntries) {
            if (!e.time().isBlank()) {
                timeCounts.computeIfAbsent(e.period(), k -> new HashMap<>())
                        .merge(e.time(), 1, Integer::sum);
            }
        }

        Map<Integer, Map<String, List<Entry>>> grid = new TreeMap<>();
        Map<Integer, Map<String, List<String>>> continuationGrid = new TreeMap<>();
        for (Entry e : found) {
            grid.computeIfAbsent(e.period(), k -> new HashMap<>())
                    .computeIfAbsent(e.day(), k -> new ArrayList<>())
                    .add(e);

            // Sessions marked by a following LAB cell occupy consecutive periods.
            // Keep the original class at its true start period and explicitly show
            // its continuation in each subsequent period row.
            for (int offset = 1; offset < e.durationPeriods(); offset++) {
                int continuationPeriod = e.period() + offset;
                if (continuationPeriod > TIMES.size()) break;
                String continuation = (allProgrammes ? "[" + e.sheet() + "] " : "")
                        + "↳ CONTINUES: " + e.course()
                        + " [" + String.join(" / ", e.groupList()) + "]"
                        + " (from P" + e.period() + ")";
                continuationGrid.computeIfAbsent(continuationPeriod, k -> new HashMap<>())
                        .computeIfAbsent(e.day(), k -> new ArrayList<>())
                        .add(continuation);
            }
        }

        List<String[]> rows = new ArrayList<>();
        for (int period = 1; period <= TIMES.size(); period++) {
            String[] cells = new String[DAYS.size() + 1];
            String periodTime = TIMES.get(period - 1);
            Map<String, Integer> choices = timeCounts.get(period);
            if (choices != null && !choices.isEmpty()) {
                periodTime = choices.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(periodTime);
            }

            cells[0] = "P" + period + "\n" + periodTime;
            Map<String, List<Entry>> dayEntries = grid.getOrDefault(period, Map.of());

            for (int i = 0; i < DAYS.size(); i++) {
                List<Entry> entries = dayEntries.getOrDefault(DAYS.get(i), List.of());
                List<String> cellText = new ArrayList<>();
                entries.stream()
                        .map(e -> formatEntry(e, allProgrammes))
                        .forEach(cellText::add);
                cellText.addAll(continuationGrid
                        .getOrDefault(period, Map.of())
                        .getOrDefault(DAYS.get(i), List.of()));
                cells[i + 1] = String.join("\n\n", cellText);
            }
            rows.add(cells);
        }

        visible.setAll(rows);

        long lectures = found.stream().filter(e -> e.hasType("L")).count();
        long tutorials = found.stream().filter(e -> e.hasType("T")).count();
        long practicals = found.stream().filter(e -> e.hasType("P")).count();
        long combined = found.stream().filter(Entry::isCombined).count();
        long continuationPeriods = found.stream()
                .mapToLong(e -> Math.max(0, e.durationPeriods() - 1))
                .sum();

        status.setText(found.size() + " timetable cells  ·  "
                + lectures + " lecture-tagged  ·  "
                + tutorials + " tutorial-tagged  ·  "
                + practicals + " practical-tagged  ·  "
                + combined + " combined cells  ·  "
                + continuationPeriods + " continuation periods");
    }

    private boolean matchesSelectedGroup(
            Entry entry,
            String selectedGroup,
            String selectedSheet,
            List<Entry> sheetEntries
    ) {
        if (entry.hasGroup(selectedGroup)) return true;

        // The PG sheet has a different programme/group structure: its labels
        // are descriptive, not parent/subgroup codes, so filter it exactly.
        if ("PG TIME TABLE".equalsIgnoreCase(selectedSheet)) return false;

        String lectureParent = sheetEntries.stream()
                .filter(e -> e.hasType("L"))
                .flatMap(e -> e.groupList().stream())
                .filter(g -> !g.isBlank() && selectedGroup.startsWith(g))
                .max(Comparator.comparingInt(String::length))
                .orElse("");

        if (lectureParent.isBlank()) return false;

        if (selectedGroup.equals(lectureParent)) {
            // Selecting a lecture parent shows its own lectures and all child groups.
            return entry.groupList().stream().anyMatch(g ->
                    g.equals(lectureParent) || g.startsWith(lectureParent));
        }

        // Selecting one tutorial/practical subgroup shows that subgroup plus
        // the parent lecture timetable, not its sibling subgroups.
        return entry.hasType("L") && entry.hasGroup(lectureParent);
    }

    private String typeLabel(String classTypes) {
        List<String> labels = new ArrayList<>();
        List<String> types = classTypes == null || classTypes.isBlank()
                ? List.of()
                : Arrays.asList(classTypes.split("\\|"));

        if (types.contains("L")) labels.add("Lecture");
        if (types.contains("T")) labels.add("Tutorial");
        if (types.contains("P")) labels.add("Practical / Lab");

        if (labels.isEmpty()) return "Class";
        if (labels.size() == 1) return labels.get(0);
        return "Combined: " + String.join(" + ", labels);
    }

    private String formatEntry(Entry e, boolean allProgrammes) {
        StringBuilder b = new StringBuilder();

        if (allProgrammes) {
            b.append('[').append(e.sheet()).append("] ");
        }

        b.append(typeLabel(e.classTypes()));
        if (!e.groupList().isEmpty()) {
            b.append("\nGroup: ").append(String.join(" / ", e.groupList()));
        }
        b.append("\n").append(e.course());

        if (e.durationPeriods() > 1) {
            b.append("\nDuration: ").append(e.durationPeriods()).append(" consecutive periods");
        }
        if (!e.room().isBlank()) {
            b.append("\nRoom / facility: ").append(e.room());
        }
        return b.toString();
    }
}
