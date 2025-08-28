package com.shared.kafka_starter.core;

public final class TopicNaming {
	  private TopicNaming(){}
	  public static String events(String env, String domain) {
	    return env + "." + domain + ".events";
	  }
	  public static String dlt(String topic) { return topic + ".dlt"; }
	}