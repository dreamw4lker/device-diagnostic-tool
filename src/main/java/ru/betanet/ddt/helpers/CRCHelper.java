package ru.betanet.ddt.helpers;

public class CRCHelper {
    /**
     * CRC16 calculation method
     *
     * @param buffer data buffer
     * @param start start index in buffer
     * @param length count of bytes
     * @return CRC16 value
     */
    public static int calculateCRC16(final byte[] buffer, final int start, final int length) {
        int Sum = (int) 0xffff;
        for (int i = start; i < length; i++) {
            short b = (short) (Byte.toUnsignedInt(buffer[i]) & 0xFF);
            Sum = (Sum ^ b);
            for (int j = 0; j < 8; j++) {
                if ((Sum & 0x1) == 1) {
                    Sum >>>= 1;
                    Sum = (Sum ^ 0xA001);
                } else {
                    Sum >>>= 1;
                }
                Sum = Sum & 0xFFFF;
            }
        }
        return Sum;
    }
    /**
     * CRC8 calculation method
     * TODO: untested method
     *
     * @param buffer data buffer
     * @param start start index in buffer
     * @param length count of bytes
     * @return CRC8 value
     */
    public static byte calculateCRC8(final byte[] buffer, final int start, final int length){
        byte crc = (byte)0xFF;
        for (int i = start; i < length; i++) {
            crc ^= buffer[i];
            for (int j = 0; j < 8; j++) {
                if((crc & 0x80) != 0){
                    crc = (byte)(crc << 1);
                    crc = (byte)(crc ^ 0x31);
                } else {
                    crc = (byte)(crc << 1);
                }
            }
        }
        return crc;
    }

    /**
     * CRC-8-Dallas/Maxim (1-Wire bus) calculation method
     *
     * @param buffer data buffer
     * @param start start index in buffer
     * @param length count of bytes
     * @return CRC8/MAXIM value
     */
    public static byte calculateCRC8_MAXIM(final byte[] buffer, final int start, final int length){
        byte crc = 0;
        for (int i = start; i < length; i++) {
            byte iter = (byte)(buffer[i] ^ crc);
            crc = 0;
            if ((iter & 0x01) != 0)
                crc ^= 0x5E;
            if ((iter & 0x02) != 0)
                crc ^= 0xBC;
            if ((iter & 0x04) != 0)
                crc ^= 0x61;
            if ((iter & 0x08) != 0)
                crc ^= 0xC2;
            if ((iter & 0x10) != 0)
                crc ^= 0x9D;
            if ((iter & 0x20) != 0)
                crc ^= 0x23;
            if ((iter & 0x40) != 0)
                crc ^= 0x46;
            if ((iter & 0x80) != 0)
                crc ^= 0x8C;
        }
        return crc;
    }

    /**
     * BCC calculation method (used in Energomera CE electricity meters)
     *
     * Calculation started from first 0x01 (Start of Heading symbol) or 0x02 (Start of Text) symbol.
     *
     * @param buffer data buffer
     * @return BCC value
     */
    public static int calculateBCC(final byte[] buffer) {
        int sum = (int) 0x0;
        int startIndex = 1;
        for(int i = 0; i < buffer.length - 1; i++) {
            if(buffer[i] == 0x01 || buffer[i] == 0x02) {
                startIndex = i + 1;
                break;
            }
        }
        for (int i = startIndex; i < buffer.length - 1; i++) {

            sum += buffer[i];
        }
        return (sum & 0x7f);
    }

    /**
     * Checking for a match of CRC16 in data buffer
     *
     * @param data data buffer
     * @return true if CRC matches, false otherwise
     */
    public static boolean checkResponseCRC16(byte[] data) {
        if (data == null) {
            return false;
        }
        if (data.length < 3) {
            return false;
        }

        int crc = calculateCRC16(data, 0, data.length - 2);
        int crc2 = unsignedBytesToInt(data[data.length - 1], data[data.length - 2]);

        return crc2 == crc;
    }

    /**
     * Checking for a match of CRC8 in data buffer
     * TODO: untested
     *
     * @param data data buffer
     * @return true if CRC matches, false otherwise
     */
    public static boolean checkResponseCRC8(byte[] data) {
        if (data == null) {
            return false;
        }
        if (data.length < 2) {
            return false;
        }

        int crc = calculateCRC8(data, 0, data.length - 1);
        int crc2 = data[data.length - 1];

        return crc2 == crc;
    }

    /**
     * Checking for a match of CRC-8/Maxim in data buffer
     *
     * @param data data buffer
     * @return true if CRC matches, false otherwise
     */
    public static boolean checkResponseCRC8_MAXIM(byte[] data) {
        if (data == null) {
            return false;
        }
        if (data.length < 2) {
            return false;
        }

        int crc = calculateCRC8_MAXIM(data, 0, data.length - 1);
        int crc2 = data[data.length - 1];

        return crc2 == crc;
    }

    /**
     * Checking for a match of BCC in data buffer
     * @param data data buffer
     * @return true if CRC matches, false otherwise
     */
    public static boolean checkBcc(byte[] data) {
        return calculateBCC(data) == data[data.length - 1];
    }

    /**
     * Unsigned bytes to integer converter method
     *
     * @param highByte high byte
     * @param lowByte low byte
     * @return value in range of [0; 255*256+255]
     */
    public static Integer unsignedBytesToInt(byte highByte, byte lowByte) {
        return Byte.toUnsignedInt(highByte) * 256 + Byte.toUnsignedInt(lowByte);
    }
}
