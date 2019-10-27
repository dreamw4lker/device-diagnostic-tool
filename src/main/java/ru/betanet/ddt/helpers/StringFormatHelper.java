package ru.betanet.ddt.helpers;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringFormatHelper {
    /**
     * @param data array of bytes
     * @return string interpretation of bytes in decimal form. Example: "1 3 5 -53"
     */
    public static String convertByteArrayToString(byte[] data) {
        if (data == null) {
            return "null data";
        }
        return IntStream.range(0, data.length).mapToObj(i -> Byte.toString(data[i])).collect(Collectors.joining(" "));
    }

    /**
     * @param data array of bytes
     * @return string interpretation of bytes in HEX form. Example: " 01 03 05 CB"
     */
    public static String convertByteArrayToHEXString(byte[] data) {
        if (data == null) {
            return "null data";
        }
        return IntStream.range(0, data.length).mapToObj(i -> String.format("%02X", data[i] & 0xFF)).collect(Collectors.joining(" "));
    }
}
