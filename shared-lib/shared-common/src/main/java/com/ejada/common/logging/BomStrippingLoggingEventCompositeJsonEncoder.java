package com.ejada.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.IOException;
import java.io.OutputStream;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

/**
 * Logback encoder that removes any UTF-8 BOM bytes emitted by logstash-logback before the
 * payload is written. Some collectors reject JSON entries that start with the optional BOM.
 */
public class BomStrippingLoggingEventCompositeJsonEncoder
        extends LoggingEventCompositeJsonEncoder {

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
