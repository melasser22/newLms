package com.ejada.template.exception;

public class SendGridSyncException extends RuntimeException {
  public SendGridSyncException(String message) {
    super(message);
  }

  public SendGridSyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
