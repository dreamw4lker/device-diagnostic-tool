package ru.betanet.ddt.helpers;

/**
 * ModBus RTU helper class
 */
public class ModBusRTUHelper {
    /**
     * ModBus RTU request creation method
     *
     * @param address device ModBus address
     * @param function function code
     * @param registerAddress first register address
     * @param registersCount registers count
     * @return prepared array of bytes
     */
    public static byte[] createRequest(int address, int function, int registerAddress, int registersCount) {
        byte[] request = new byte[8];
        request[0] = (byte) address;
        request[1] = (byte) function;
        request[2] = getHighByteFromInteger(registerAddress);
        request[3] = getLowByteFromInteger(registerAddress);
        request[4] = getHighByteFromInteger(registersCount);
        request[5] = getLowByteFromInteger(registersCount);
        int crc16 = CRCHelper.calculateCRC16(request, 0, 6);
        request[6] = getLowByteFromInteger(crc16);
        request[7] = getHighByteFromInteger(crc16);
        return request;
    }

    /**
     * @return high byte from integer value
     */
    public static byte getHighByteFromInteger(Integer value) {
        return (byte) (value >> 8);
    }

    /**
     * @return low byte from integer value
     */
    public static byte getLowByteFromInteger(Integer value) {
        return (byte) (value % 256);
    }
}
