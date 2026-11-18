package com.university.scheduler.ui;

import com.university.scheduler.dao.UiRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/** Read-only browser for the original nine-sheet university timetable imported into MySQL. */
public final class OfficialTimetableView {
    private static final int PAGE_SIZE = 200;
    private final ComboBox<String> sheet = new ComboBox<>();
    private final TextField search = new TextField();
    private final TableView<String[]> table = new TableView<>();
    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private final Label status = new Label("Choose a sheet to browse its imported cells.");
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private int page = 0;

    public Parent getView() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        Label note = new Label("Read-only view of the original Excel timetable stored in MySQL. Search a course code, room, faculty initials, or section label.");
        note.setWrapText(true);
        sheet.setPromptText("Select timetable sheet");
        sheet.setPrefWidth(220);
        search.setPromptText("Search within selected sheet (e.g. UCS503L, 2Q1, LT102)");
        HBox filters = new HBox(10, new Label("Sheet:"), sheet, search);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        table.getColumns().addAll(column("Excel row", 0), column("Cell", 1), column("Column #", 2), column("Cell value", 3));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(rows);
        table.setPlaceholder(new Label("No matching cells. Select a sheet or change the search."));
        VBox.setVgrow(table, Priority.ALWAYS);

        previous.setOnAction(e -> { if (page > 0) { page--; load(); } });
        next.setOnAction(e -> { page++; load(); });
        Button apply = new Button("Search");
        apply.getStyleClass().add("primary-button");
        apply.setOnAction(e -> { page = 0; load(); });
        search.setOnAction(e -> { page = 0; load(); });
        sheet.setOnAction(e -> { page = 0; load(); });
        HBox paging = new HBox(10, previous, next, status);
        paging.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(note, filters, apply, table, paging);
        loadSheets();
        return root;
    }

    private TableColumn<String[], String> column(String title, int index) {
        return UiSupport.col(title, r -> r[index]);
    }

    private void loadSheets() {
        try {
            List<String> names = UiRepository.sourceTimetableSheets();
            sheet.setItems(FXCollections.observableArrayList(names));
            if (!names.isEmpty()) {
                sheet.getSelectionModel().selectFirst();
                load();
            } else {
                status.setText("No imported source timetable found. Check timetable_source_cells in scheduler_db.");
            }
        } catch (Exception e) {
            UiSupport.error("Could not load imported timetable sheets", e);
        }
    }

    private void load() {
        String selected = sheet.getValue();
        if (selected == null || selected.isBlank()) return;
        try {
            List<String[]> result = UiRepository.sourceTimetableCells(selected, search.getText().trim(), PAGE_SIZE, page * PAGE_SIZE);
            rows.setAll(result);
            previous.setDisable(page == 0);
            next.setDisable(result.size() < PAGE_SIZE);
            status.setText("Page " + (page + 1) + " · " + result.size() + " cells shown" + (search.getText().isBlank() ? "" : " · filtered by '" + search.getText().trim() + "'"));
        } catch (Exception e) {
            UiSupport.error("Could not read timetable cells", e);
        }
    }
}
