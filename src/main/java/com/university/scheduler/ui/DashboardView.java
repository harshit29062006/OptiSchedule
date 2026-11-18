package com.university.scheduler.ui;

import com.university.scheduler.dao.UiRepository;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DashboardView {
    public Parent getView() {
        VBox root = UiSupport.page("Dashboard", "Your university scheduling workspace — manage resources and view the official weekly timetable.");
        Label status = new Label("Database and timetable statistics"); status.getStyleClass().add("section-heading");
        HBox cards = new HBox(12);
        cards.getChildren().addAll(card("Rooms", "rooms"), card("Faculty", "faculty"),
                card("Courses", "courses"), card("Scheduled sessions", "bookings"),
                valueCard("Official weekly classes", parsedSlotCount()));
        for (var n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        HBox.setHgrow(cards, Priority.ALWAYS);
        Label help = new Label("Use Weekly Timetable to view the official Monday–Sunday schedule by year and section, reconstructed from the supplied July–December 2026 Excel workbook. Booking Manager is for manually managed sessions.");
        help.setWrapText(true);
        VBox note = new VBox(10, new Label("Getting started"), help); note.getStyleClass().add("content-card");
        root.getChildren().addAll(status, cards, note);
        return root;
    }

    private VBox card(String label, String table) {
        int value;
        try { value = UiRepository.count(table); }
        catch (Exception e) { VBox error = valueCard(label, "DB error"); Tooltip.install(error, new Tooltip(e.getMessage())); return error; }
        return valueCard(label, String.valueOf(value));
    }

    private VBox valueCard(String label, String value) {
        Label name = new Label(label); name.getStyleClass().add("stat-title");
        Label number = new Label(value); number.getStyleClass().add("stat-number");
        VBox box = new VBox(10, name, number); box.getStyleClass().add("stat-card");
        box.setPadding(new Insets(15)); box.setMinWidth(120); return box;
    }

    private String parsedSlotCount() {
        try (InputStream in = getClass().getResourceAsStream("/data/weekly_timetable.csv")) {
            if (in == null) return "Missing";
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                long count = r.lines().skip(1).filter(s -> !s.isBlank()).count();
                return String.valueOf(count);
            }
        } catch (Exception e) { return "—"; }
    }
}
