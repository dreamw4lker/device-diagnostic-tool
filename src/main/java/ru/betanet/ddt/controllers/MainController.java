package ru.betanet.ddt.controllers;

import com.google.common.primitives.Bytes;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import ru.betanet.ddt.dto.DeviceDataDTO;
import ru.betanet.ddt.helpers.CRCHelper;
import ru.betanet.ddt.helpers.ModBusRTUHelper;
import ru.betanet.ddt.services.DeviceExchangeService;

import java.net.URL;
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

    @FXML
    private TextArea deviceLog;

    @FXML
    private CheckBox printInHEXCheckbox;

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
    private Label connectionStatus;

    @FXML
    private void handleTestConnection(ActionEvent event) {
        if (!validateIpPortFields()) return;
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    connectionStatus.setText("Please wait...");
                });
                DeviceExchangeService des = new DeviceExchangeService();
                boolean isConnectionSuccessful = des.isPortAvailable(deviceIP.getText(), Integer.parseInt(devicePort.getText()));
                Platform.runLater(() -> {
                    if (isConnectionSuccessful) {
                        connectionStatus.setText("Connection is OK");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    deviceLog.appendText(String.format("<< %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), e.getMessage()));
                    connectionStatus.setText("Connection failed");
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
                DeviceExchangeService des = new DeviceExchangeService();
                DeviceDataDTO ddDTO = des.runExchange(deviceIP.getText(), Integer.parseInt(devicePort.getText()), request, true);
                Platform.runLater(() -> {
                    if (printInHEXCheckbox.isSelected()) {
                        deviceLog.appendText(String.format(">> %s\n", convertByteArrayToHEXString(ddDTO.requestData)));
                        deviceLog.appendText(String.format("<< %s\n", convertByteArrayToHEXString(ddDTO.responseData)));
                    } else {
                        deviceLog.appendText(String.format(">> %s\n", convertByteArrayToString(ddDTO.requestData)));
                        deviceLog.appendText(String.format("<< %s\n", convertByteArrayToString(ddDTO.responseData)));
                    }
                });
            } catch (Exception e) {
                deviceLog.appendText(String.format("<< %s:%s - %s\n", deviceIP.getText(), Integer.parseInt(devicePort.getText()), e.getMessage()));
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
