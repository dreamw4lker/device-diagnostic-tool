package ru.betanet.ddt.services;

import ru.betanet.ddt.dto.DeviceDataDTO;
import ru.betanet.ddt.dto.ResponseType;
import ru.betanet.ddt.helpers.CRCHelper;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class DeviceExchangeService {
    /**
     * Socket opening timeout in ms
     */
    private final static int DEFAULT_OPEN_TIMEOUT = 5_000;
    /**
     * Socket lifetime in ms
     */
    private final static int DEFAULT_NETWORK_TIMEOUT = 10_000;

    private final int socketOpenTimeout;
    private final int socketOperationTimeout;

    public DeviceExchangeService() {
        this.socketOpenTimeout = DEFAULT_OPEN_TIMEOUT;
        this.socketOperationTimeout = DEFAULT_NETWORK_TIMEOUT;
    }

    public DeviceExchangeService(int networkTimeout) {
        this.socketOpenTimeout = networkTimeout;
        this.socketOperationTimeout = networkTimeout;
    }

    public DeviceExchangeService(int socketOpenTimeout, int socketOperationTimeout) {
        this.socketOpenTimeout = socketOpenTimeout;
        this.socketOperationTimeout = socketOperationTimeout;
    }

    private boolean isBreakByCrc(boolean breakOnCRC, byte[] inputBuffer) {
        return breakOnCRC && CRCHelper.checkResponseCRC16(inputBuffer);
    }

    public Socket openSocket(String address, int port) throws Exception {
        synchronized (this) {
            Socket deviceSocket = new Socket();
            deviceSocket.connect(new InetSocketAddress(address, port), socketOpenTimeout);
            deviceSocket.setTcpNoDelay(true);
            deviceSocket.setSoTimeout(socketOperationTimeout);
            return deviceSocket;
        }
    }

    public DeviceDataDTO runExchange(String address, int port, byte[] request, boolean breakOnCRC) throws Exception {
        try (Socket deviceSocket = openSocket(address, port)) {
            return runExchange(deviceSocket, request, breakOnCRC);
        }
    }

    private DeviceDataDTO runExchange(final Socket deviceSocket, byte[] request, boolean breakOnCRC) throws Exception {
        synchronized (this) {
            DeviceDataDTO ddDTO = new DeviceDataDTO();
            ddDTO.deviceAddress = String.format("%s:%s",
                    deviceSocket.getInetAddress().toString().substring(1),
                    deviceSocket.getPort());
            ddDTO.requestData = request;

            //Send request and flush stream
            deviceSocket.getOutputStream().write(request);
            deviceSocket.getOutputStream().flush();
            //Sleep for Ethernet-to-COM converters
            Thread.sleep(50);
            //Preparing for read from remote device
            InputStream devIn = new BufferedInputStream(deviceSocket.getInputStream(), 65535);
            int local_counter;
            byte[] inputBuffer = new byte[0];
            byte[] temporalBuffer = new byte[65536];

            //Partial read to the end of a stream. May be interrupted on SocketTimeout or by CRC match
            try {
                while ((local_counter = devIn.read(temporalBuffer, 0, temporalBuffer.length)) > 0) {
                    byte[] tbuff = new byte[inputBuffer.length + local_counter];
                    if (inputBuffer.length > 0) {
                        System.arraycopy(inputBuffer, 0, tbuff, 0, inputBuffer.length);
                    }
                    System.arraycopy(temporalBuffer, 0, tbuff, inputBuffer.length, local_counter);
                    inputBuffer = new byte[tbuff.length];
                    System.arraycopy(tbuff, 0, inputBuffer, 0, tbuff.length);
                    //Breaking on CRC match
                    if (isBreakByCrc(breakOnCRC, inputBuffer)) {
                        ddDTO.responseType = ResponseType.OK_CRC;
                        break;
                    }
                    //Breaking on ModBus max frame size reach
                    if (inputBuffer.length >= 256) {
                        ddDTO.responseType = ResponseType.ERROR_MAXLEN;
                        break;
                    }
                }
                if (ddDTO.responseType == null) {
                    ddDTO.responseType = ResponseType.OK_EOS;
                }
                ddDTO.responseData = inputBuffer;
            } catch(SocketTimeoutException e) {
                ddDTO.responseType = ResponseType.SOCKET_TIMEOUT;
                ddDTO.responseData = inputBuffer;
            }
            return ddDTO;
        }
    }

    public boolean isPortAvailable(String address, int port) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), DEFAULT_OPEN_TIMEOUT);
            return true;
        }
    }
}
