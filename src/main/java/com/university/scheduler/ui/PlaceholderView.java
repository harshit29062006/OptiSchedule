
package com.university.scheduler.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class PlaceholderView {

    public static StackPane create(String screenName) {

        Label label = new Label(
            screenName + " Management"
        );

        label.getStyleClass().add("placeholder-title");

        StackPane root = new StackPane(label);
        root.getStyleClass().add("placeholder-screen");
        root.setAlignment(Pos.CENTER);

        return root;
    }
}
