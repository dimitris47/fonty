/**
 * Copyright 2021-2022 Dimitris Psathas <dimitrisinbox@gmail.com>
 *
 * This file is part of Fonty.
 *
 * Fonty is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License  as  published by  the  Free Software
 * Foundation,  either version 3 of the License,  or (at your option)  any later
 * version.
 *
 * Fonty is distributed in the hope that it will be useful,  but  WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the  GNU General Public License  for more details.
 *
 * You should have received a copy of the  GNU General Public License along with
 * Fonty. If not, see <http://www.gnu.org/licenses/>.
 */


package com.dimitris47.fonty;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class Fonty extends Application {
    Preferences prefs;
    double defWidth, defHeight;

    Label lblFontFamily, lblFontSize, statusLabel;
    ComboBox<String> combo;
    Button loadButton, reset, info;
    ToggleButton toggle;
    Spinner<Integer> spinner;
    CheckBox cbBold, cbItalic;
    TextArea text;
    String defText;

    static File openedFile;
    Font openedFont;
    static String argFont;
    static String fallbackStatus;

    @Override
    public void start(Stage stage) {
        prefs = Preferences.userNodeForPackage(Fonty.class);
        defWidth = 1024;
        defHeight = 768;
        Font defFont = new Font(14);

        defText = """
                The quick brown fox jumps over the lazy dog
                                
                ABCDEFGHIJKLMNOPQRSTUVWXYZ????
                abcdefghijklmnopqrstuvwxyz????
                                
                1234567890.:,;!?'*&()
                                
                ???????? ?????????? ?????????????????????????? ?????????? ????????????????????
                                
                ????????????????????????????????????????????????
                ????????????????????????????????????????????????????????????????????""";

        fallbackStatus = "If selected font does not support all displayed characters, " +
                "the system font will be used as a fallback.";

        lblFontFamily = new Label("Font Family:");

        combo = new ComboBox<>();
        combo.getItems().addAll(Font.getFamilies());
        combo.setValue(combo.getItems().get(0));
        combo.setPrefWidth(224);
        combo.getEditor().setFont(defFont);
        combo.getEditor().setAlignment(Pos.CENTER_LEFT);
        combo.setOnAction(e -> {
            if (!toggle.isSelected())
                setSelFont();
        });
        combo.setOnScroll(e -> {
            int deltaY = (int) e.getDeltaY();
            if (deltaY > 0) {
                try {
                    Robot r = new Robot();
                    r.keyPress(java.awt.event.KeyEvent.VK_UP);
                    r.keyRelease(java.awt.event.KeyEvent.VK_UP);
                } catch (AWTException exc) {
                    exc.printStackTrace();
                }
            } else if (deltaY < 0) {
                try {
                    Robot r = new Robot();
                    r.keyPress(java.awt.event.KeyEvent.VK_DOWN);
                    r.keyRelease(java.awt.event.KeyEvent.VK_DOWN);
                } catch (AWTException exc) {
                    exc.printStackTrace();
                }
            }
        });

        lblFontSize = new Label("Size:");

        spinner = new Spinner<>();
        spinner.setMaxWidth(80);
        spinner.setEditable(false);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 124, 42, 8));
        spinner.getEditor().setFont(defFont);
        spinner.getEditor().setAlignment(Pos.CENTER);
        spinner.setOnMouseClicked(e -> {
            try {
                spinnerClicked(stage);
            } catch (IOException | FontFormatException exc) {
                exc.printStackTrace();
            }
        });
        spinner.setOnKeyPressed(e -> {
            try {
                spinnerClicked(stage);
            } catch (IOException | FontFormatException exc) {
                exc.printStackTrace();
            }
        });
        spinner.setOnScroll((ScrollEvent e) -> {
            int deltaY = (int) e.getDeltaY();
            if (deltaY > 0) {
                spinner.getValueFactory().setValue(spinner.getValue() + 8);
            } else if (deltaY < 0) {
                spinner.getValueFactory().setValue(spinner.getValue() - 8);
            }
            try {
                spinnerClicked(stage);
            } catch (IOException | FontFormatException exc) {
                exc.printStackTrace();
            }
        });

        cbBold = new CheckBox("Bold");
        cbBold.setOnAction(e -> setSelFont());
        cbItalic = new CheckBox("Italic");
        cbItalic.setOnAction(e -> setSelFont());

        loadButton = new Button("Load font");
        loadButton.setOnAction(e -> {
            try {
                if (loadFont(stage)) {
                    System.out.println("Loaded from app: " + openedFile.toString());
                    toggle.setSelected(true);
                    combo.setDisable(true);
                    cbBold.setDisable(true);
                    cbItalic.setDisable(true);
                    statusLabel.setText("");
                }
            } catch (IOException | FontFormatException exc) {
                exc.printStackTrace();
            }
        });

        toggle = new ToggleButton("File mode");
        toggle.setSelected(false);
        toggle.setOnAction(e -> {
            if (toggle.isSelected()) {
                combo.setDisable(true);
                cbBold.setDisable(true);
                cbItalic.setDisable(true);
            } else {
                combo.setDisable(false);
                cbBold.setDisable(false);
                cbItalic.setDisable(false);
                stage.setTitle("Fonty");
                setSelFont();
                statusLabel.setText(fallbackStatus);
            }
        });

        reset = new Button("Reset text");
        reset.setOnAction(e -> text.setText(defText));

        info = new Button("Info");
        info.setOnAction(e -> {
            String info = """
                Program created by Dimitris Psathas

                Written in Java, utilizing the JavaFX toolkit

                Published under the GPLv3 License
                
                \u00A9 2021-2022 Dimitris Psathas""";

            Alert infoDialog = new Alert(Alert.AlertType.INFORMATION);
            infoDialog.setTitle("Program Info");
            infoDialog.setResizable(true);
            infoDialog.setHeaderText("Fonty");
            infoDialog.setContentText(info);
            infoDialog.initOwner(stage);
            infoDialog.showAndWait();
        });

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(8, 8, 0, 8));
        hBox.setSpacing(8);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().addAll(lblFontFamily, combo, lblFontSize, spinner, new Separator(),
                cbItalic, cbBold, new Separator(), loadButton, toggle, new Separator(),
                reset, new Separator(), info);

        text = new TextArea();
        text.setWrapText(true);
        text.minHeightProperty().bind(stage.heightProperty().subtract(108));
        text.setPadding(new Insets(8));

        statusLabel = new Label();
        statusLabel.setPadding(new Insets(0, 0, 8, 8));
        HBox statusBar = new HBox();
        statusBar.setMinHeight(16);
        statusBar.setMaxHeight(24);
        statusBar.getChildren().add(statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox();
        box.setSpacing(8);
        box.getChildren().addAll(hBox, text, statusBar);

        for (var node : Arrays.asList(
                lblFontFamily, lblFontSize, cbBold, cbItalic, loadButton, toggle, reset, info, statusLabel))
            node.setFont(defFont);

        Scene scene = new Scene(box, defWidth, defHeight);
        stage.setScene(scene);
        stage.setMinWidth(defWidth);
        stage.setMinHeight(defHeight);
        stage.setTitle("Fonty");
        stage.getIcons().add(new Image("fonty.png"));

        getPrefs(stage);
        stage.setOnCloseRequest(e -> setPrefs(stage));
        stage.show();
        setSelFont();
        getArgFont(stage);
        statusLabel.setText(fallbackStatus);
    }

    private void getArgFont(Stage stage) {
        if (argFont != null) {
            openFile(stage);
            text.setFont(openedFont);
            toggle.setSelected(true);
            combo.setDisable(true);
            cbBold.setDisable(true);
            cbItalic.setDisable(true);
        }
    }

    private void openFile(Stage stage) {
        File file = new File(String.valueOf(Paths.get(
                URI.create("file:///" + argFont.replace(" ", "%20").replace("\\", "/")))));
        try {
            openedFont = Font.loadFont(new FileInputStream(file), 42);
            stage.setTitle("Fonty - " + file.getName());
        } catch (FileNotFoundException exc) {
            exc.printStackTrace();
        }
    }

    private boolean loadFont(Stage stage) throws IOException, FontFormatException {
        boolean loaded = false;
        openedFile = new File(String.valueOf(new FileChooser().showOpenDialog(stage)));
        try {
            openedFont = Font.loadFont(new FileInputStream(openedFile), spinner.getValue());
            stage.setTitle("Fonty - " + openedFile.getName());
            text.setFont(openedFont);
            loaded = true;
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return loaded;
    }

    private void spinnerClicked(Stage stage) throws IOException, FontFormatException {
        if (toggle.isSelected()) {
            if (argFont != null) {
                openFile(stage);
            }
            text.setFont(Font.loadFont(new FileInputStream(openedFile), spinner.getValue()));
            statusLabel.setText("");
        } else {
            setSelFont();
            statusLabel.setText(fallbackStatus);
        }
    }

    private void setSelFont() {
        Font f = Font.font(
                combo.getValue(),
                cbBold.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL,
                cbItalic.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR,
                spinner.getValue());
        text.setFont(f);
    }

    private void setPrefs(Stage stage) {
        final String locX = "locationX";
        prefs.put(locX, String.valueOf(stage.getX()));
        final String locY = "locationY";
        prefs.put(locY, String.valueOf(stage.getY()));
        final String stWidth = "width";
        String currWidth = String.valueOf(stage.getWidth());
        prefs.put(stWidth, currWidth);
        final String stHeight = "height";
        String currHeight = String.valueOf(stage.getHeight());
        prefs.put(stHeight, currHeight);
        final String isMax = "isMax";
        prefs.put(isMax, String.valueOf(stage.isMaximized()));
        final String usrTxt = "usrTxt";
        prefs.put(usrTxt, text.getText());
    }

    private void getPrefs(Stage stage) {
        final String isMax = prefs.get("isMax", "false");
        if (isMax.equals("true")) {
            stage.setMaximized(true);
        } else {
            final double savedX = Double.parseDouble(prefs.get("locationX", "128.0"));
            final double savedY = Double.parseDouble(prefs.get("locationY", "64.0"));
            stage.setX(savedX);
            stage.setY(savedY);
            final double savedWidth = Double.parseDouble(prefs.get("width", String.valueOf(defWidth)));
            final double savedHeight = Double.parseDouble(prefs.get("height", String.valueOf(defHeight)));
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
        }
        final String usrTxt = prefs.get("usrTxt", defText);
        text.setText(usrTxt);
    }

    public static void main(String[] args) {
        try {
            argFont = args[0];
            openedFile = new File(argFont);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No file loaded as argument");
        }
        launch(args);
    }
}
