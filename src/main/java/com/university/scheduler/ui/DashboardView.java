
package com.university.scheduler.ui;

import com.university.scheduler.dao.UiRepository;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DashboardView {

    public Parent getView() {

        VBox root = UiSupport.page(
                "Welcome to OptiSchedule",
                "Plan, schedule, manage and optimize your university resources."
        );

        root.setSpacing(22);

        // ==========================
        // WELCOME BANNER
        // ==========================

        Label welcomeTitle = new Label("Smarter Academic Scheduling");
        welcomeTitle.getStyleClass().add("welcome-title");

        Label welcomeText = new Label(
                "Manage university resources and explore your official "
                + "weekly timetable from one workspace."
        );

        welcomeText.getStyleClass().add("welcome-description");
        welcomeText.setWrapText(true);

        VBox welcomeContent = new VBox(12,
                welcomeTitle,
                welcomeText
        );

        welcomeContent.setMaxWidth(650);

        VBox banner = new VBox(welcomeContent);
        banner.getStyleClass().add("welcome-banner");
        banner.setPadding(new Insets(25));
        banner.setMinHeight(130);

        // ==========================
        // STATISTICS
        // ==========================

        Label sectionTitle = new Label("Overview");
        sectionTitle.getStyleClass().add("section-heading");

        HBox statistics = new HBox(14);

        statistics.getChildren().addAll(
                createCard("ROOMS", "▣", "rooms", "Available resources"),
                createCard("FACULTY", "♙", "faculty", "Teaching staff"),
                createCard("COURSES", "▤", "courses", "Academic courses"),
                createCard("BOOKINGS", "◷", "bookings", "Managed sessions"),
                createOfficialCard()
        );

        statistics.setAlignment(Pos.CENTER_LEFT);

        for (var card : statistics.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        // ==========================
        // INFORMATION PANEL
        // ==========================

        Label timetableHeading = new Label(
                "Official Weekly Timetable"
        );
        timetableHeading.getStyleClass().add("section-heading");

        Label timetableDescription = new Label(
                "Explore the July–December 2026 university timetable. "
                + "The timetable includes lectures, tutorials, practicals "
                + "and multi-period laboratory sessions."
        );

        timetableDescription.getStyleClass().add("dashboard-description");
        timetableDescription.setWrapText(true);

        Label sourceLabel = new Label(
                "DATA SOURCE   •   OFFICIAL UNIVERSITY WORKBOOK"
        );

        sourceLabel.getStyleClass().add("source-label");

        VBox timetableInfo = new VBox(
                12,
                timetableHeading,
                timetableDescription,
                sourceLabel
        );

        timetableInfo.getStyleClass().add("content-card");
        timetableInfo.setPadding(new Insets(23));

        // ==========================
        // FOOTER INFORMATION
        // ==========================

        Label footer = new Label(
                "OPTISCHEDULE 2.0   •   ACADEMIC SCHEDULING SYSTEM"
        );

        footer.getStyleClass().add("dashboard-footer");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(
                banner,
                sectionTitle,
                statistics,
                timetableInfo,
                spacer,
                footer
        );

        return root;
    }

    private VBox createCard(
            String title,
            String icon,
            String table,
            String description
    ) {

        String value;

        try {
            value = String.valueOf(UiRepository.count(table));
        } catch (Exception e) {
            value = "—";
        }

        return buildCard(title, icon, value, description);
    }

    private VBox createOfficialCard() {

        return buildCard(
                "OFFICIAL CLASSES",
                "▦",
                parsedSlotCount(),
                "Source timetable entries"
        );
    }

    private VBox buildCard(
            String title,
            String icon,
            String value,
            String description
    ) {

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-number");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("stat-description");

        VBox card = new VBox(
                12,
                iconLabel,
                titleLabel,
                valueLabel,
                descriptionLabel
        );

        card.getStyleClass().add("stat-card");

        card.setPadding(new Insets(17));
        card.setMinWidth(130);
        card.setPrefWidth(180);
        card.setMinHeight(175);

        return card;
    }

    private String parsedSlotCount() {

        try (InputStream in =
                     getClass().getResourceAsStream(
                             "/data/weekly_timetable.csv"
                     )) {

            if (in == null) {
                return "Missing";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
            )) {

                long count = reader.lines()
                        .skip(1)
                        .filter(line -> !line.isBlank())
                        .count();

                return String.valueOf(count);
            }

        } catch (Exception e) {
            return "—";
        }
    }
}
