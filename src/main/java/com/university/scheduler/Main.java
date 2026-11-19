
package com.university.scheduler;

import com.university.scheduler.ui.DashboardView;
import com.university.scheduler.ui.PlaceholderView;
import com.university.scheduler.ui.RoomView;
import com.university.scheduler.ui.FacultyView;
import com.university.scheduler.ui.CourseView;
import com.university.scheduler.ui.TimetableView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends Application {

    private BorderPane layout;
    private Label pageTitle;
    private final Map<String, Button> navigation = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {

        layout = new BorderPane();
        layout.getStyleClass().add("main-layout");

        // ==========================
        // SIDEBAR
        // ==========================

        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(26, 16, 22, 16));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(245);
        sidebar.setMinWidth(220);

        Label logo = new Label("◈  OptiSchedule");
        logo.getStyleClass().add("logo");

        Label subtitle = new Label("ACADEMIC MANAGEMENT");
        subtitle.getStyleClass().add("sidebar-section");

        VBox brand = new VBox(5, logo, subtitle);
        brand.setPadding(new Insets(0, 5, 28, 5));

        Label workspaceLabel = new Label("WORKSPACE");
        workspaceLabel.getStyleClass().add("sidebar-section");

        Button dashboardBtn = createNavButton("⌂", "Dashboard");
        Button timetableBtn = createNavButton("▦", "Timetable");
        Button roomsBtn = createNavButton("▣", "Rooms");
        Button facultyBtn = createNavButton("♙", "Faculty");
        Button coursesBtn = createNavButton("▤", "Courses");

        Label managementLabel = new Label("MANAGEMENT");
        managementLabel.getStyleClass().add("sidebar-section");

        Button bookingsBtn = createNavButton("◷", "Bookings");
        Button analyticsBtn = createNavButton("▥", "Analytics");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = createNavButton("⚙", "Settings");
        Button helpBtn = createNavButton("?", "Help");

        sidebar.getChildren().addAll(
                brand,
                workspaceLabel,
                dashboardBtn,
                timetableBtn,
                bookingsBtn,
                managementLabel,
                roomsBtn,
                facultyBtn,
                coursesBtn,
                analyticsBtn,
                spacer,
                settingsBtn,
                helpBtn
        );

        layout.setLeft(sidebar);

        // ==========================
        // TOP HEADER
        // ==========================

        pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("page-title");

        Label dateLabel = new Label(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
                )
        );
        dateLabel.getStyleClass().add("date-label");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label notification = new Label("●");
        notification.getStyleClass().add("notification-dot");

        Label userAvatar = new Label("AD");
        userAvatar.getStyleClass().add("user-avatar");

        Label userLabel = new Label("Administrator");
        userLabel.getStyleClass().add("user-label");

        VBox userInfo = new VBox(2,
                userLabel,
                new Label("Academic Admin")
        );
        userInfo.getStyleClass().add("user-info");

        HBox profile = new HBox(10, userAvatar, userInfo);
        profile.setAlignment(Pos.CENTER);

        HBox header = new HBox(
                18,
                pageTitle,
                headerSpacer,
                dateLabel,
                notification,
                profile
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(17, 28, 17, 28));
        header.getStyleClass().add("header");

        layout.setTop(header);

        // ==========================
        // NAVIGATION ACTIONS
        // ==========================

        dashboardBtn.setOnAction(e ->
                showPage("Dashboard", dashboardBtn));

        timetableBtn.setOnAction(e ->
                showPage("Timetable", timetableBtn));

        bookingsBtn.setOnAction(e ->
                showPage("Bookings", bookingsBtn));

        roomsBtn.setOnAction(e ->
                showPage("Rooms", roomsBtn));

        facultyBtn.setOnAction(e ->
                showPage("Faculty", facultyBtn));

        coursesBtn.setOnAction(e ->
                showPage("Courses", coursesBtn));

        analyticsBtn.setOnAction(e ->
                showPage("Analytics", analyticsBtn));

        settingsBtn.setOnAction(e ->
                showPage("Settings", settingsBtn));

        helpBtn.setOnAction(e ->
                showPage("Help", helpBtn));

        // ==========================
        // INITIAL PAGE
        // ==========================

        showPage("Dashboard", dashboardBtn);

        Scene scene = new Scene(layout, 1440, 900);

        var css = getClass().getResource("/styles.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("OptiSchedule | Academic Scheduling System");
        stage.setScene(scene);

        stage.setMinWidth(1050);
        stage.setMinHeight(700);

        stage.setMaximized(true);
        stage.show();
    }

    private Button createNavButton(String icon, String name) {

        Button button = new Button(icon + "     " + name);

        button.getStyleClass().add("nav-button");

        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefHeight(46);

        navigation.put(name, button);

        return button;
    }

    private void showPage(String name, Button selected) {

        pageTitle.setText(name);

        switch (name) {

            case "Dashboard":
                layout.setCenter(new DashboardView().getView());
                break;

            case "Timetable":
                layout.setCenter(new TimetableView().getView());
                break;

            case "Rooms":
                layout.setCenter(new RoomView().getView());
                break;

            case "Faculty":
                layout.setCenter(new FacultyView().getView());
                break;

            case "Courses":
                layout.setCenter(new CourseView().getView());
                break;

            default:
                layout.setCenter(PlaceholderView.create(name));
                break;
        }

        navigation.values().forEach(button ->
                button.getStyleClass().remove("active")
        );

        selected.getStyleClass().add("active");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
