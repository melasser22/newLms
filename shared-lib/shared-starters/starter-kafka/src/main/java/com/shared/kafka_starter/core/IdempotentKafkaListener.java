package com.shared.kafka_starter.core;

import org.apache.kafka.clients.consumer.ConsumerRecord;


public abstract class IdempotentKafkaListener<T> {
  private final IdempotencyStore store;
  private final String groupId;
  private final long ttlSeconds;

  protected IdempotentKafkaListener(IdempotencyStore store, String groupId, long ttlSeconds) {
    this.store = store; this.groupId = groupId; this.ttlSeconds = ttlSeconds;
  }

  public final void handle(ConsumerRecord<String, T> record) {
	  String messageId = header(record, Headers.MESSAGE_ID);
	  if (messageId == null) {
	    if (record.key() != null) {
	      messageId = record.key();
	    } else {
	      messageId = record.topic() + "-" + record.partition() + "-" + record.offset();
	    }
	  }
	  if (!store.putIfAbsent(groupId, messageId, ttlSeconds)) {
	    // duplicate – skip
	    return;
	  }
	  onMessage(record);
	}
  
  private static String header(ConsumerRecord<?, ?> r, String name) {
	  var h = r.headers().lastHeader(name);
	  return h != null ? new String(h.value()) : null;
	}

  protected abstract void onMessage(ConsumerRecord<String, T> record);

  
}
