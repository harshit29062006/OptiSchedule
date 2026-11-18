
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private BorderPane layout;
    private Label pageTitle;

    @Override
    public void start(Stage stage) {

        layout = new BorderPane();
        layout.getStyleClass().add("main-layout");

        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(25, 15, 25, 15));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label logo = new Label("OptiSchedule");
        logo.getStyleClass().add("logo");

        Button dashboardBtn = createNavButton("Dashboard");
        Button roomsBtn = createNavButton("Rooms");
        Button facultyBtn = createNavButton("Faculty");
        Button coursesBtn = createNavButton("Courses");
        Button timetableBtn = createNavButton("Timetable");

        sidebar.getChildren().addAll(
                logo,
                dashboardBtn,
                roomsBtn,
                facultyBtn,
                coursesBtn,
                timetableBtn
        );

        pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("page-title");

        Label userLabel = new Label("Admin");
        userLabel.getStyleClass().add("user-label");

        HBox header = new HBox(pageTitle, userLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);
        header.setPadding(new Insets(20, 30, 20, 30));
        HBox.setHgrow(pageTitle, javafx.scene.layout.Priority.ALWAYS);
        header.getStyleClass().add("header");

        layout.setLeft(sidebar);
        layout.setTop(header);

        dashboardBtn.setOnAction(e -> showPage("Dashboard", dashboardBtn));
        roomsBtn.setOnAction(e -> showPage("Rooms", roomsBtn));
        facultyBtn.setOnAction(e -> showPage("Faculty", facultyBtn));
        coursesBtn.setOnAction(e -> showPage("Courses", coursesBtn));
        timetableBtn.setOnAction(e -> showPage("Timetable", timetableBtn));

        showPage("Dashboard", dashboardBtn);

        Scene scene = new Scene(layout, 1200, 750);

        var css = getClass().getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("OptiSchedule - University Scheduling System");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.show();
    }

    private Button createNavButton(String name) {
        Button button = new Button(name);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        return button;
    }

    private void showPage(String name, Button selected) {

        pageTitle.setText(name);

        switch (name) {
            case "Dashboard":
                layout.setCenter(new DashboardView().getView());
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

            case "Timetable":
                layout.setCenter(new TimetableView().getView());
                break;

            default:
                layout.setCenter(PlaceholderView.create(name));
                break;

        }

        layout.getLeft().lookupAll(".nav-button")
                .forEach(node -> node.getStyleClass().remove("active"));

        selected.getStyleClass().add("active");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
