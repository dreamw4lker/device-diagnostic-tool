package ru.betanet.ddt.controllers;

import com.google.common.primitives.Bytes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.betanet.ddt.dto.DeviceDataDTO;
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
    private void handleTestConnection(ActionEvent event) {
        try {
            //TODO: implement test method
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @FXML
    private void handleSendModBusRequest(ActionEvent event) {
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
        try {
            byte[] request = Bytes.toArray(
                    Arrays.stream(directRequestData.getText().split(" "))
                            .map(elem -> (byte)Integer.parseInt(elem, 16))
                            .collect(Collectors.toList())
            );
            sendRequest(request);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    /**
     * Sends request ro remote device and fill exchange log in form's text area
     *
     * @param request array of bytes
     * @throws Exception device exchange exception
     */
    private void sendRequest(byte[] request) throws Exception {
        DeviceExchangeService des = new DeviceExchangeService();
        DeviceDataDTO ddDTO = des.runExchange(deviceIP.getText(), Integer.parseInt(devicePort.getText()), request, true);
        if (printInHEXCheckbox.isSelected()) {
            deviceLog.appendText(String.format(">> %s\n", convertByteArrayToHEXString(ddDTO.requestData)));
            deviceLog.appendText(String.format("<< %s\n", convertByteArrayToHEXString(ddDTO.responseData)));
        } else {
            deviceLog.appendText(String.format(">> %s\n", convertByteArrayToString(ddDTO.requestData)));
            deviceLog.appendText(String.format("<< %s\n", convertByteArrayToString(ddDTO.responseData)));
        }
    }
}
