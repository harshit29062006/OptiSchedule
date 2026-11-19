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

    VBox root = new VBox(18);
    root.setPadding(new Insets(22));
    root.setStyle("-fx-background-color: #F0DAAE;");

    // Page heading
    Label heading = new Label("Weekly Timetable");
    heading.setStyle(
        "-fx-font-family: 'Georgia';" +
        "-fx-font-size: 28px;" +
        "-fx-font-weight: bold;" +
        "-fx-text-fill: #482E1D;"
    );

    Label subtitle = new Label(
        "Official university schedule · July–December 2026"
    );
    subtitle.setStyle(
        "-fx-font-size: 13px;" +
        "-fx-text-fill: #895D2B;"
    );

    VBox titleBox = new VBox(5, heading, subtitle);

    // Statistics and class type legend
    Label lectureLegend = createLegend("Lecture", "#895D2B");
    Label tutorialLegend = createLegend("Tutorial", "#A3966A");
    Label practicalLegend = createLegend("Practical", "#90553C");

    HBox legend = new HBox(
        18,
        lectureLegend,
        tutorialLegend,
        practicalLegend
    );
    Button compactBtn = new Button("Compact");
Button comfortableBtn = new Button("Comfortable");

compactBtn.getStyleClass().add("primary-button");
comfortableBtn.getStyleClass().add("primary-button");

compactBtn.setOnAction(e -> {
    table.setFixedCellSize(55);
});

comfortableBtn.setOnAction(e -> {
    table.setFixedCellSize(85);
});

HBox viewControls = new HBox(
    10,
    new Label("VIEW DENSITY:"),
    compactBtn,
    comfortableBtn
);

viewControls.setAlignment(Pos.CENTER_RIGHT);
    legend.setAlignment(Pos.CENTER_LEFT);

    // Filters
    sheet.setPromptText("Select programme");
    sheet.setPrefWidth(210);

    group.setPromptText("Select section");
    group.setPrefWidth(180);

    search.setPromptText("Search course, room, group or faculty");
    search.setPrefHeight(40);
    HBox.setHgrow(search, Priority.ALWAYS);

    Button apply = new Button("Apply Filters");
    apply.getStyleClass().add("primary-button");
    apply.setPrefHeight(40);
    apply.setOnAction(e -> refresh());

    HBox filters = new HBox(
        12,
        sheet,
        group,
        search,
        apply
    );
    filters.setAlignment(Pos.CENTER_LEFT);

    // Timetable table
    table.getColumns().clear();

    table.getColumns().add(
        column("PERIOD / TIME", 0, 125)
    );

    for (int i = 0; i < DAYS.size(); i++) {
        table.getColumns().add(
            column(DAYS.get(i).toUpperCase(), i + 1, 145)
        );
    }

    table.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY
    );

    table.setFixedCellSize(76);
    table.setMinHeight(480);
    table.setPrefHeight(650);
    table.setMaxHeight(Double.MAX_VALUE);

    VBox.setVgrow(table, Priority.ALWAYS);
    table.setItems(visible);
    table.setPlaceholder(
        new Label("No classes match your selected filters.")
    );

    table.getStyleClass().add("modern-timetable");

    VBox.setVgrow(table, Priority.ALWAYS);

    // Status
    status.setWrapText(true);
    status.setStyle(
        "-fx-text-fill: #895D2B;" +
        "-fx-font-size: 12px;"
    );

    // Filter actions
    sheet.setOnAction(e -> loadGroups());
    group.setOnAction(e -> refresh());
    search.setOnAction(e -> refresh());

    // Main content card
    VBox timetableCard = new VBox(
    12,
    filters,
    legend,
    viewControls,
    table,
    status
);

    timetableCard.setPadding(new Insets(18));
    timetableCard.setStyle(
        "-fx-background-color: #FFF8EA;" +
        "-fx-background-radius: 16;" +
        "-fx-border-color: #E0CBA7;" +
        "-fx-border-radius: 16;"
    );

    root.getChildren().add(timetableCard);

    loadData();

    return root;
}

private Label createLegend(String name, String color) {

    Label label = new Label("●  " + name);

    label.setStyle(
        "-fx-text-fill: " + color + ";" +
        "-fx-font-size: 12px;" +
        "-fx-font-weight: bold;"
    );

    return label;
}


    
private TableColumn<String[], String> column(
        String title,
        int index,
        double width
) {

    TableColumn<String[], String> c =
            new TableColumn<>(title);

    c.setCellValueFactory(
        v -> new SimpleStringProperty(v.getValue()[index])
    );

    c.setPrefWidth(width);

    c.setCellFactory(col -> new TableCell<>() {

        private final VBox card = new VBox(5);
        private final Label heading = new Label();
        private final Label details = new Label();

        {
            card.setPadding(new Insets(8));
            card.setMinHeight(90);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setAlignment(Pos.CENTER_LEFT);

            heading.setWrapText(true);
            heading.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #482E1D;"
            );

            details.setWrapText(true);
            details.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: #482E1D;"
            );

            card.getChildren().addAll(heading, details);
        }

        @Override
        protected void updateItem(String value, boolean empty) {

            super.updateItem(value, empty);

            if (empty || value == null || value.isBlank()) {
                setGraphic(null);
                setStyle("-fx-background-color: #FFF8EA;");
                return;
            }

            if (index == 0) {

                heading.setText(value);
                details.setText("");

                card.setStyle(
                    "-fx-background-color: #F0DAAE;" +
                    "-fx-background-radius: 8;"
                );

            } else {

                String[] lines = value.split("\\n");

                String firstLine = lines.length > 0
                        ? lines[0] : "Class";

                heading.setText(firstLine);

                StringBuilder remaining = new StringBuilder();

                for (int i = 1; i < lines.length; i++) {
                    if (remaining.length() > 0) {
                        remaining.append("\n");
                    }
                    remaining.append(lines[i]);
                }

                details.setText(remaining.toString());

                String background = "#F8E5C5";

                if (value.contains("Tutorial")) {
                    background = "#E8E6CC";
                }

                if (value.contains("Practical")
                        || value.contains("Lab")) {
                    background = "#EBD0C5";
                }

                if (value.contains("CONTINUES")) {
                    background = "#E7D8B8";
                }

                if (value.contains("Combined:")) {
                    background = "#E6D8B9";
                }

                card.setStyle(
                    "-fx-background-color: " + background + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #E0CBA7;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 0.5;"
                );
            }

            setGraphic(card);
            setStyle(
                "-fx-background-color: #FFF8EA;" +
                "-fx-padding: 3;"
            );
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
