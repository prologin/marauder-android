package org.prologin.marauder;

/**
 * Thrown when required configuration is not available.
 */
public class UnconfiguredException extends Exception {
  UnconfiguredException() {
    super();
  }

  UnconfiguredException(String message) {
    super(message);
  }
}
