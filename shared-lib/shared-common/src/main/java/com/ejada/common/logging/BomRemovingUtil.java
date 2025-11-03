package com.ejada.common.logging;

import java.util.Arrays;

final class BomRemovingUtil {
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private BomRemovingUtil() {}

    static byte[] strip(byte[] input) {
        if (input == null || input.length < UTF8_BOM.length) {
            return input;
        }
        if ((input[0] == UTF8_BOM[0]) && (input[1] == UTF8_BOM[1]) && (input[2] == UTF8_BOM[2])) {
            return Arrays.copyOfRange(input, UTF8_BOM.length, input.length);
        }
        return input;
    }
}
