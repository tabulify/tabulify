package com.tabulify.exception;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class
 * A class that will rethrow the same wrapped type class
 * if this a runtime one or wrap it in a {@link RuntimeException}
 * and add context.
 */
public class ExceptionWrapper {


  public enum ContextPosition {
    FIRST,
    LAST
  }

  /**
   * Static builder class for adding context to exceptions
   */
  public static class Builder<T extends Throwable> {
    /**
     * A throwable and not an exception
     * because we may pass through the {@link Exception#getCause()}
     * that is a {@link Throwable}
     */
    private final T originalThrowable;
    private final List<String> contextMessages;
    private ContextPosition position;
    private String separator;

    private Builder(T originalThrowable, String context) {
      this.originalThrowable = originalThrowable;
      this.contextMessages = new ArrayList<>();
      this.position = ContextPosition.LAST; // default
      this.separator = System.lineSeparator(); // default
      this.addContext(context);
    }

    /**
     * Add a context message
     */
    public Builder<T> addContext(String context) {
      if (context != null && !context.trim().isEmpty()) {
        this.contextMessages.add(context);
      }
      return this;
    }

    /**
     * Set the position where context should be added
     */
    public Builder<T> setPosition(ContextPosition position) {
      this.position = position;
      return this;
    }

    /**
     * Set the separator between context and original message
     */
    public Builder<T> setSeparator(String separator) {
      this.separator = separator;
      return this;
    }


    /**
     * Build and return the enhanced exception
     */
    public RuntimeException buildAsRuntimeException() {
      String enhancedMessage = buildEnhancedMessage();

      // If original is already a RuntimeException, enhance it
      if (originalThrowable instanceof RuntimeException) {
        return (RuntimeException) createEnhancedException(enhancedMessage);
      } else {
        // Wrap checked exception in RuntimeException
        return new RuntimeException(enhancedMessage, originalThrowable);
      }
    }

    /**
     * Build and return the enhanced exception (original behavior)
     */
    public T buildAsOriginalException() {

      if (contextMessages.isEmpty()) {
        return originalThrowable;
      }

      String enhancedMessage = buildEnhancedMessage();
      return createEnhancedException(enhancedMessage);
    }


    /**
     * Build enhanced message from context and original message
     */
    private String buildEnhancedMessage() {
      if (contextMessages.isEmpty()) {
        return originalThrowable.getMessage();
      }

      String originalMessage = originalThrowable.getMessage();
      String contextString = String.join(separator, contextMessages);

      if (position == ContextPosition.FIRST) {
        return contextString + (originalMessage != null ? separator + originalMessage : "");
      } else {
        return (originalMessage != null ? originalMessage + separator : "") + contextString;
      }
    }

    /**
     * Create enhanced RuntimeException preserving original exception details
     */
    private RuntimeException createEnhancedRuntimeException(String enhancedMessage, RuntimeException original) {
      try {
        Class<? extends RuntimeException> exceptionClass = original.getClass();

        // Try constructor with message and cause
        try {
          Constructor<? extends RuntimeException> constructor = exceptionClass.getConstructor(String.class, Throwable.class);
          RuntimeException newException = constructor.newInstance(enhancedMessage, original);
          // set stack trace otherwise we lost it
          newException.setStackTrace(original.getStackTrace());
          return newException;
        } catch (NoSuchMethodException e) {
          // Try constructor with just message
          Constructor<? extends RuntimeException> constructor = exceptionClass.getConstructor(String.class);
          RuntimeException newException = constructor.newInstance(enhancedMessage);
          // set stack trace otherwise we lost it
          newException.setStackTrace(original.getStackTrace());
          return newException;
        }
      } catch (Exception e) {
        // Fallback to generic RuntimeException
        RuntimeException fallback = new RuntimeException(enhancedMessage, original);
        fallback.setStackTrace(original.getStackTrace());
        return fallback;
      }
    }


    private T createEnhancedException(String enhancedMessage) {
      try {
        Class<? extends Throwable> exceptionClass = originalThrowable.getClass();

        // Try constructor with message and cause
        try {
          Constructor<? extends Throwable> constructor = exceptionClass.getConstructor(String.class, Throwable.class);
          @SuppressWarnings("unchecked")
          T newException = (T) constructor.newInstance(enhancedMessage, originalThrowable.getCause());
          newException.setStackTrace(originalThrowable.getStackTrace());
          return newException;
        } catch (NoSuchMethodException e) {
          // Try constructor with just message
          Constructor<? extends Throwable> constructor = exceptionClass.getConstructor(String.class);
          @SuppressWarnings("unchecked")
          T newException = (T) constructor.newInstance(enhancedMessage);
          newException.setStackTrace(originalThrowable.getStackTrace());
          if (originalThrowable.getCause() != null) {
            newException.initCause(originalThrowable.getCause());
          }
          return newException;
        }
      } catch (Exception e) {
        // If reflection fails, return original exception
        return originalThrowable;
      }
    }
  }

  /**
   * Create a builder for the given exception
   */
  public static <T extends Throwable> Builder<T> builder(T throwable, String context) {
    return new Builder<>(throwable, context);
  }


}
