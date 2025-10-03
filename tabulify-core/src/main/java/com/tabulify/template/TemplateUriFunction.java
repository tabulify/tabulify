package com.tabulify.template;


import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.*;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoPathFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Calculate a final data path from:
 * * a templated data uri
 * * a source
 * * a pipeline
 * * a record stream
 *
 * <p>
 * Usage:
 * * This is a helper function used to determine a data uri
 * * It can also be used in a {@link java.util.stream.Stream stream} as intermediate operation
 * <p>
 * Example: It supports template target uri
 * - if the data path was selected with a glob selector, it has therefore as attribute the value 1
 * - the target $1 would take this value
 * <p>
 * ie if there is a data resource called `foo` selected with `*i`, the target will be `foi` with `$1i`
 * <p>
 * <p>
 */
public class TemplateUriFunction implements Function<DataPath, DataPath> {


  private final Map<DataPath, DataPath> dataPathMap = new HashMap<>();
  private final TargetUriFunctionBuilder builder;


  public TemplateUriFunction(TargetUriFunctionBuilder builder) {
    this.builder = builder;
  }

  public static TargetUriFunctionBuilder builder(Tabular tabular) {
    return new TargetUriFunctionBuilder(tabular);
  }


  /**
   * May throw an Illegal Argument Exception
   *
   * @param inputDataPath the function argument
   */
  @Override
  public DataPath apply(DataPath inputDataPath) {

    return apply(inputDataPath, TemplateMetas.builder().addInputDataPath(inputDataPath));


  }

  private DataPath getTarget(DataPath referenceDataPath, TemplateMetas templateMetas) {

    Objects.requireNonNull(referenceDataPath, "The reference data paths should be non-null");


    /**
     * Empty target returns an empty memory data path
     * (used by the diff when the query should return nothing)
     */
    DataUriNode targetUri = this.builder.targetUri;
    Tabular tabular = this.builder.tabular;

    if (targetUri == null) {
      return tabular.getMemoryConnection()
        .getDataPath(referenceDataPath.getLogicalName())
        .mergeDataDefinitionFrom(referenceDataPath);
    }

    /**
     * Calculating the target
     */
    final KeyNormalizer targetConnectionName = targetUri.getConnection().getName();
    final Connection targetConnection;

    if (targetConnectionName == null) {
      // It may be an uri (ie https...)
      try {
        String path = targetUri.getPath();
        URI uri = URI.create(path);
        return tabular.getDataPath(Paths.get(uri));
      } catch (IllegalArgumentException e) {
        // This is not an uri
        DbLoggers.LOGGER_DB_ENGINE.fine("This is not an URI selector. Error: ", e.getMessage());
        DbLoggers.LOGGER_DB_ENGINE.fine("This is not an URI selector, setting the default datastore to " + tabular.getDefaultConnection());
        targetConnection = tabular.getDefaultConnection();
      } catch (NoPathFoundException e) {
        throw new IllegalStateException("There is no connection and no path for the target uri (" + targetUri + "), one of them is mandatory");
      }
    } else {
      targetConnection = tabular.getConnection(targetConnectionName);
      if (targetConnection == null) {
        String expectedConnections = tabular.getConnections().stream()
          .map(Connection::getName)
          .map(KeyNormalizer::toString)
          .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("The target connection name (" + targetConnectionName + ") from the target data uri (" + targetUri + ") was not found.\n" +
          "We were expecting one of: " + expectedConnections);
      }
    }

    if (targetConnection == null) {
      throw new InternalException("The connection from the target data uri (" + targetUri + ") should not be null");
    }

    String targetPath;
    try {
      targetPath = targetUri.getPath();
      if (targetPath.isEmpty()) {
        targetPath = null;
      }
    } catch (NoPathFoundException e) {
      targetPath = null;
    }


    /**
     * Media type
     */
    MediaType mediaType = this.builder.targetMediaType;
    if (mediaType == null && this.builder.targetIsContainer) {
      mediaType = targetConnection.getDataSystem().getContainerMediaType();
    }

    // Target path definition
    DataPath targetDataPath;
    if (targetPath != null) {

      /**
       * Template
       */
      if (TextTemplateEngine.isTextTemplate(targetPath)) {

        String templateTargetPath = this.builder.templateString.apply(templateMetas);
        targetDataPath = targetConnection.getDataPath(templateTargetPath, mediaType);

      } else {
        /**
         * This is the case for instance
         * when this is a transfer from one data resource to another
         */
        targetDataPath = targetConnection.getDataPath(targetPath, mediaType);
      }

    } else {

      /**
       * Determine a target from a source data resource
       */
      if (!this.builder.targetIsContainer) {
        DataSystem dataSystem = targetConnection.getDataSystem();
        targetDataPath = dataSystem.getTargetFromSource(referenceDataPath, mediaType, null);
      } else {
        targetDataPath = targetConnection.getCurrentDataPath();
      }

    }

    // If the target is not a container, we take a child if the calculated target is a container
    if (!this.builder.targetIsContainer) {
      if (Tabulars.isContainer(targetDataPath) && !Tabulars.isContainer(referenceDataPath)) {
        targetDataPath = targetConnection.getDataSystem().getTargetFromSource(referenceDataPath, null, targetDataPath);
      }
    }

    return targetDataPath;

  }

  public DataPath apply(DataPath referenceDataPath, TemplateMetas templateMetas) {

    DataPath targetDataPath = getTarget(referenceDataPath, templateMetas);

    /**
     * Make sure we use the same target
     * ie when using a {@link com.tabulify.memory.MemoryDataPath}
     * the data is in it.
     * <p>
     * Synchronized to be sure we don't get any java.util.ConcurrentModificationException
     * as contains and put are atomic
     */
    synchronized (this.dataPathMap) {
      if (this.dataPathMap.containsKey(targetDataPath)) {
        targetDataPath = this.dataPathMap.get(targetDataPath);
      } else {
        this.dataPathMap.put(targetDataPath, targetDataPath);
      }
    }

    /**
     * Set the data definition (target properties, columns) if any
     */
    targetDataPath.mergeDataDefinitionFromYamlMap(this.builder.targetDataDef);

    return targetDataPath;
  }


  public static class TargetUriFunctionBuilder {

    private final Tabular tabular;
    public TemplateString templateString;
    private DataUriNode targetUri;
    private Map<KeyNormalizer, ?> targetDataDef = new HashMap<>();
    //private Pipeline pipeline;

    /**
     * Do we return a container data path?
     */
    private Boolean targetIsContainer;
    /**
     * Strictness
     */
    private Boolean isStrict = null;

    private MediaType targetMediaType;
    private Set<KeyNormalizer> extraTemplatePrefixes = new HashSet<>();
    private Meta pipeline;


    public TargetUriFunctionBuilder(Tabular tabular) {
      this.tabular = tabular;
    }

    public TargetUriFunctionBuilder setTargetUri(DataUriNode targetUri) {
      this.targetUri = targetUri;
      return this;
    }


    public TargetUriFunctionBuilder setTargetDataDef(Map<KeyNormalizer, ?> targetDataDef) {
      this.targetDataDef = targetDataDef;
      return this;
    }

    public TargetUriFunctionBuilder setStrict(Boolean strict) {
      isStrict = strict;
      return this;
    }

    public TemplateUriFunction build() {

      if (isStrict == null) {
        isStrict = this.tabular.isStrictExecution();
      }

      if (targetMediaType == null) {
        Object mediaTypeObject = this.targetDataDef.get(KeyNormalizer.createSafe(DataPathAttribute.MEDIA_TYPE));
        if (mediaTypeObject != null) {
          if (!(mediaTypeObject instanceof String)) {
            throw new IllegalArgumentException("The target media type specified in the target data definition is not a string but a " + mediaTypeObject.getClass().getName());
          }
          String mediaType = (String) mediaTypeObject;
          try {
            targetMediaType = MediaTypes.parse(mediaType);
          } catch (NullValueException e) {
            throw new IllegalArgumentException("The target media type is null");
          }
        }
      }

      /**
       * Target is container
       */
      if (this.targetIsContainer == null) {
        if (targetMediaType != null) {
          this.targetIsContainer = targetMediaType.isContainer();
        } else {
          this.targetIsContainer = false;
        }
      }

      /**
       * TargetTemplate building
       */
      String path;
      try {
        path = this.targetUri.getPath();
      } catch (NoPathFoundException e) {
        path = "";
      }
      this.templateString = TemplateString
        .builder(path)
        .setExtraPrefixes(this.extraTemplatePrefixes)
        .isStrict(this.isStrict)
        .setPipeline(pipeline)
        .build();
      return new TemplateUriFunction(this);
    }

    public TargetUriFunctionBuilder setPipeline(Meta pipeline) {
      this.pipeline = pipeline;
      return this;
    }

    /**
     * @param targetIsContainer - true if the returned data path should be a container
     */
    public TargetUriFunctionBuilder setTargetIsContainer(boolean targetIsContainer) {
      this.targetIsContainer = targetIsContainer;
      return this;
    }

    public TargetUriFunctionBuilder setTargetMediaType(MediaType targetMediaType) {
      this.targetMediaType = targetMediaType;
      return this;
    }

    public TargetUriFunctionBuilder setExtraTemplatePrefixes(Set<KeyNormalizer> extraTemplatesPrefixes) {
      this.extraTemplatePrefixes = extraTemplatesPrefixes;
      return this;
    }

  }
}
