package ru.betanet.ddt.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import ru.betanet.ddt.App;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * About window controller
 */
public class AboutController implements Initializable {
    @FXML
    private Label versionLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.versionLabel.setText("version: " + App.class.getPackage().getImplementationVersion());
    }

    @FXML
    private void handleClose(ActionEvent event) {
        ((Node)event.getSource()).getScene().getWindow().hide();
    }
}
