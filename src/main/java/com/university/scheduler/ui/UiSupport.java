package com.university.scheduler.ui;
import javafx.geometry.Insets;import javafx.scene.Parent;import javafx.scene.control.*;import javafx.scene.layout.*;import javafx.beans.property.SimpleStringProperty;import java.sql.SQLException;
final class UiSupport{
 private UiSupport(){}
 static VBox page(String title,String subtitle){VBox v=new VBox(16);v.setPadding(new Insets(26));v.getStyleClass().add("dashboard");Label h=new Label(title);h.getStyleClass().add("page-heading");Label s=new Label(subtitle);s.getStyleClass().add("page-subtitle");VBox card=new VBox(12);card.getStyleClass().add("content-card");v.getChildren().addAll(h,s);return v;}
 static void error(String msg,Throwable e){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle("OptiSchedule");a.setHeaderText(msg);a.setContentText(e.getMessage()==null?e.toString():e.getMessage());a.showAndWait();}
 static void info(String msg){Alert a=new Alert(Alert.AlertType.INFORMATION,msg,ButtonType.OK);a.setHeaderText(null);a.showAndWait();}
 static boolean confirm(String msg){Alert a=new Alert(Alert.AlertType.CONFIRMATION,msg,ButtonType.YES,ButtonType.NO);return a.showAndWait().orElse(ButtonType.NO)==ButtonType.YES;}
 static <T> TableColumn<T,String> col(String name,java.util.function.Function<T,String> getter){TableColumn<T,String> c=new TableColumn<>(name);c.setCellValueFactory(d->new SimpleStringProperty(getter.apply(d.getValue())));return c;}
 static void style(TableView<?> t){t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);t.setPlaceholder(new Label("No records found. Add a record or import your data."));}
 static TextField field(String prompt){TextField f=new TextField();f.setPromptText(prompt);return f;}
}
