package com.shared.moneytime.starter.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.shared.moneytime.starter.money.MoneyUtils;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.math.BigDecimal;

public class SharedMoneyModule extends SimpleModule {

  public SharedMoneyModule() {
    super("SharedMoneyModule");
    addSerializer(MonetaryAmount.class, new MonetaryAmountJsonSerializer());
    addDeserializer(MonetaryAmount.class, new MonetaryAmountJsonDeserializer());
  }

  static class MonetaryAmountJsonSerializer extends JsonSerializer<MonetaryAmount> {
    @Override
    public void serialize(MonetaryAmount value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      if (value == null) { gen.writeNull(); return; }
      gen.writeStartObject();
      gen.writeStringField("amount", MoneyUtils.amount(value).toPlainString());
      gen.writeStringField("currency", MoneyUtils.currency(value));
      gen.writeEndObject();
    }
  }

  static class MonetaryAmountJsonDeserializer extends JsonDeserializer<MonetaryAmount> {
    @Override
    public MonetaryAmount deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.currentToken() == JsonToken.VALUE_STRING) {
        String s = p.getValueAsString();
        String[] parts = s.trim().split("\\s+");
        if (parts.length == 2) return MoneyUtils.of(new BigDecimal(parts[1]), parts[0]);
        return null;
      }
      String amountStr = null, currency = null;
      while (p.nextToken() != JsonToken.END_OBJECT) {
        String name = p.getCurrentName();
        p.nextToken();
        if ("amount".equals(name)) amountStr = p.getValueAsString();
        else if ("currency".equals(name)) currency = p.getValueAsString();
      }
      if (amountStr == null || currency == null) return null;
      return MoneyUtils.of(new BigDecimal(amountStr), currency);
    }
  }
}
