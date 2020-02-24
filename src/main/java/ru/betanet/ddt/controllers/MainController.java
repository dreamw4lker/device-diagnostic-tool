package ru.betanet.ddt.controllers;

import com.google.common.primitives.Bytes;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.StatusBar;
import ru.betanet.ddt.dto.DeviceDataDTO;
import ru.betanet.ddt.helpers.CRCHelper;
import ru.betanet.ddt.helpers.ModBusRTUHelper;
import ru.betanet.ddt.services.DeviceExchangeService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static ru.betanet.ddt.helpers.StringFormatHelper.convertByteArrayToHEXString;
import static ru.betanet.ddt.helpers.StringFormatHelper.convertByteArrayToString;

/**
 * Main window controller
 */
public class MainController implements Initializable {

    @FXML
    private TextField deviceIP;

    @FXML
    private TextField devicePort;

    //TODO: migrate to TextFlow?
    @FXML
    private TextArea deviceLog;

    @FXML
    private CheckBox printInHEXCheckbox;

    @FXML
    private CheckBox showResponseStatusCheckbox;

    @FXML
    private TextField modBusDeviceID;

    @FXML
    private TextField modBusFC;

    @FXML
    private TextField modBusFirstRegisterAddress;

    @FXML
    private TextField modBusRegistersCount;

    @FXML
    private TextArea directRequestData;

    @FXML
    private StatusBar statusBar;

    @FXML
    private void handleTestConnection(ActionEvent event) {
        if (!validateIpPortFields()) return;
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    deviceLog.appendText(String.format(">> %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), "Testing connection..."));
                    statusBar.setText("Testing connection. Please wait...");
                });
                DeviceExchangeService des = new DeviceExchangeService();
                boolean isConnectionSuccessful = des.isPortAvailable(deviceIP.getText(), Integer.parseInt(devicePort.getText()));
                Platform.runLater(() -> {
                    if (isConnectionSuccessful) {
                        deviceLog.appendText(String.format("<< %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), "Connection is OK"));
                        statusBar.setText("Connection is OK");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    deviceLog.appendText(String.format("<< %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), e.getMessage()));
                    statusBar.setText("Connection failed");
                });
            }
        }).start();
    }

    @FXML
    private void handleSendModBusRequest(ActionEvent event) {
        if (!validateIpPortFields()) return;
        if (!validateModBusFields()) return;
        try {
            byte deviceID = (byte) Integer.parseInt(modBusDeviceID.getText(), 16);
            byte functionCode = (byte) Integer.parseInt(modBusFC.getText(), 16);
            int firstRegisterAddress = Integer.parseInt(modBusFirstRegisterAddress.getText(), 16);
            int registersCount = Integer.parseInt(modBusRegistersCount.getText(), 16);
            byte[] request = ModBusRTUHelper.createRequest(deviceID, functionCode, firstRegisterAddress, registersCount);
            sendRequest(request);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void handleSendDirectRequest(ActionEvent event) {
        if (!validateIpPortFields()) return;
        if (!validateDirectFields()) return;
        try {
            byte[] request = Bytes.toArray(
                    Arrays.stream(directRequestData.getText().split(" "))
                            .map(elem -> (byte) Integer.parseInt(elem, 16))
                            .collect(Collectors.toList())
            );
            sendRequest(request);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void handleInsertCRC16(ActionEvent event) {
        if (!validateDirectFields()) return;
        byte[] request = Bytes.toArray(
                Arrays.stream(directRequestData.getText().split(" "))
                        .map(elem -> (byte) Integer.parseInt(elem, 16))
                        .collect(Collectors.toList())
        );
        int crc = CRCHelper.calculateCRC16(request, 0, request.length);
        byte[] crcArray = new byte[]{ModBusRTUHelper.getLowByteFromInteger(crc), ModBusRTUHelper.getHighByteFromInteger(crc)};
        directRequestData.appendText(convertByteArrayToHEXString(crcArray));
    }

    @FXML
    private void handleFileExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleFileDump(ActionEvent event) {
        Stage currentStage = (Stage) this.deviceLog.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.setInitialFileName("ddt-dump-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHMMssSSS")));
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(currentStage);
        if (file != null) {
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(deviceLog.getText());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    @FXML
    private void handleToolsCrcCalculator(ActionEvent event) {
        new Alert(Alert.AlertType.INFORMATION, "Not implemented yet").show();
    }

    @FXML
    private void handleHelpAbout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/ru/betanet/ddt/About.fxml"));
            Parent parent = loader.load();
            Stage stage = new Stage();
            stage.setTitle("About");
            stage.setScene(new Scene(parent));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(620);
            stage.setMinHeight(390);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    /**
     * Sends request ro remote device and fill exchange log in form's text area
     *
     * @param request array of bytes
     */
    private void sendRequest(byte[] request) {
        new Thread(() -> {
            try {
                if (printInHEXCheckbox.isSelected()) {
                    deviceLog.appendText(String.format(">> %s\n", convertByteArrayToHEXString(request)));
                } else {
                    deviceLog.appendText(String.format(">> %s\n", convertByteArrayToString(request)));
                }
                Platform.runLater(() -> {
                    statusBar.setText("Sending request. Please wait...");
                });
                DeviceExchangeService des = new DeviceExchangeService();
                DeviceDataDTO ddDTO = des.runExchange(deviceIP.getText(), Integer.parseInt(devicePort.getText()), request, true);
                Platform.runLater(() -> {
                    if (printInHEXCheckbox.isSelected()) {
                        deviceLog.appendText(String.format("<< %s\n", convertByteArrayToHEXString(ddDTO.responseData)));
                    } else {
                        deviceLog.appendText(String.format("<< %s\n", convertByteArrayToString(ddDTO.responseData)));
                    }
                    if (showResponseStatusCheckbox.isSelected()) {
                        deviceLog.appendText(String.format("<< %s\n", ddDTO.responseType.getDescription()));
                    }
                    statusBar.setText("Request completed");
                });
            } catch (Exception e) {
                deviceLog.appendText(String.format("<< %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), e.getMessage()));
                Platform.runLater(() -> {
                    statusBar.setText("Device failed to response");
                });
            }
        }).start();
    }

    private boolean validateIpPortFields() {
        if (deviceIP.getText().isEmpty() || devicePort.getText().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "You should fill both IP and Port fields").show();
            return false;
        }
        return true;
    }

    private boolean validateModBusFields() {
        if (modBusDeviceID.getText().isEmpty() || modBusFC.getText().isEmpty() ||
                modBusFirstRegisterAddress.getText().isEmpty() || modBusRegistersCount.getText().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "You should fill all ModBus request fields").show();
            return false;
        }
        return true;
    }

    private boolean validateDirectFields() {
        if (directRequestData.getText().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "You should fill all direct request fields").show();
            return false;
        }
        return true;
    }
}
