package com.ejada.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.IOException;
import java.io.OutputStream;
import net.logstash.logback.encoder.LogstashEncoder;

/**
 * Companion encoder for auto-configuration that strips UTF-8 BOM bytes from each encoded
 * payload before it is written to the target appender.
 */
public class BomStrippingLogstashEncoder extends LogstashEncoder {

    @Override
    public void encode(ILoggingEvent event, OutputStream outputStream) throws IOException {
        byte[] data = encode(event);
        if (data == null || data.length == 0) {
            return;
        }
        outputStream.write(data);
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        return BomRemovingUtil.strip(super.encode(event));
    }
}
