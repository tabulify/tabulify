package com.tabulify.flow.step;


import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.flow.engine.OperationStep;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.uri.DataUri;
import net.bytle.exception.NoPathFoundException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Calculate the target for a source and returns a map of source / target.
 * <p>
 * Usage:
 * * In a flow, this is a helper function used in the {@link TransferStep#createRunnable()}() transfer operation}
 * * It can also be used in a {@link java.util.stream.Stream stream} as intermediate operation
 * <p>
 * Example: It supports template target uri
 * - if the data path was selected with a glob selector, it has therefore as attribute the value 1
 * - the target $1 would take this value
 * <p>
 * ie if there is a data resource called `foo` selected with `*i`, the target will be `foi` with `$1i`
 * <p>
 * <p>
 * Source Target cannot be an {@link OperationStep} because it produce a set of source target.
 */
public class SourceTargetHelperFunction implements Function<Set<DataPath>, Map<DataPath, DataPath>> {


  /**
   * When we move tabular data into a file system,
   * the below extension (tabular format) for the file is used
   */
  public static final MediaType FS_DEFAULT_TABULAR_MEDIA_TYPE = MediaTypes.TEXT_CSV;
  private final Tabular tabular;


  private DataUri targetUri;


  private Map<String, ?> targetDataDef = new HashMap<>();


  public SourceTargetHelperFunction(Tabular tabular) {
    this.tabular = tabular;
  }

  public static SourceTargetHelperFunction create(Tabular tabular) {
    return new SourceTargetHelperFunction(tabular);
  }


  @Override
  public Map<DataPath, DataPath> apply(Set<DataPath> sourceDataPaths) {

    Objects.requireNonNull(sourceDataPaths, "The source data paths should be non-null");
    /**
     * The Source target map to return
     */
    Map<DataPath, DataPath> sourceTargets = new HashMap<>();

    /**
     * Empty target returns an empty memory data path
     * (used by the compare when the query should return nothing)
     */
    if (targetUri == null) {
      return sourceDataPaths.stream()
        .collect(Collectors.toMap(
          dp -> dp,
          dp -> tabular.getMemoryDataStore().getDataPath(dp.getLogicalName())
            .mergeDataDefinitionFromYamlMap(this.targetDataDef)
            .mergeDataDefinitionFrom(dp)
        ));
    }

    /**
     * Calculating the target
     */
    final String targetConnectionName = targetUri.getConnection().getName();
    final Connection targetConnection;

    if (targetConnectionName == null) {
      // It may be a uri (ie https...)
      try {
        String path = this.targetUri.getPath();
        URI uri = URI.create(path);
        DataPath targetDataPath = tabular.getDataPath(Paths.get(uri));
        sourceTargets = sourceDataPaths
          .stream()
          .collect(
            Collectors.toMap(
              e -> e, // the key
              e -> targetDataPath
            ));
        return sourceTargets;
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
    }


    if (targetConnection == null) {
      throw new RuntimeException("The connection from the target data uri (" + targetUri + ") was not found");
    }


    String targetPath;
    try {
      targetPath = targetUri.getPath();
    } catch (NoPathFoundException e) {
      targetPath = null;
    }
    for (DataPath sourceDataPath : sourceDataPaths) {

      // Target path definition
      DataPath targetDataPath;
      if (targetPath != null) {

        /**
         * Template
         */
        if (TextTemplateEngine.isTextTemplate(targetPath)) {

          TextTemplate textTemplateEngine = TextTemplateEngine.getOrCreate().compile(targetPath);
          Map<String, Object> map = new HashMap<>();

          for (String attributeName : textTemplateEngine.getVariableNames()) {
            Variable variable;
            try {
              variable = sourceDataPath.getVariable(attributeName);
            } catch (NoVariableException e) {
              throw new IllegalStateException("We couldn't calculate the target resource name via backward reference. The variable (" + attributeName + ") in the pattern expression (" + targetPath + ") was not found for the resource (" + sourceDataPath + "). It can happen if this resource was selected as dependency. If this is the case, you should perform select without dependencies.");
            }
            try {
              map.put(attributeName, variable.getValueOrDefault());
            } catch (NoValueException e) {
              throw new IllegalStateException("The variable (" + attributeName + ") was found but had no value for the resource (" + sourceDataPath + ")");
            }
          }

          String templateTargetPath = textTemplateEngine.applyVariables(map).getResult();
          targetDataPath = targetConnection.getDataPath(templateTargetPath);

        } else {
          /**
           * This is the case for instance
           * when this is a transfer from one data resource to another
           */
          targetDataPath = targetConnection.getDataPath(targetPath);
        }

      } else {

        targetDataPath = createTargetNameFromSource(sourceDataPath, targetConnection);

      }

      // If the calculated target is a container, we take a child
      if (Tabulars.isContainer(targetDataPath)) {
        targetDataPath = targetDataPath.getChild(targetDataPath.getName());
      }

      // Add this source target in the map
      sourceTargets.put(sourceDataPath, targetDataPath);
    }

    /**
     * Set the data definition (target properties, columns) if any
     */
    sourceTargets.values().forEach(dp -> dp.mergeDataDefinitionFromYamlMap(targetDataDef));

    return sourceTargets;
  }

  /**
   * @param targetConnection - the target connection
   * @param sourceDataPath   - the source data path
   * @return determine a target name (not a path) from a source data resource
   * <p>
   * If the target datastore is a file system and the source is a tabular structure,
   * the target name will be the concatenation of the source name + the extension csv
   */
  static DataPath createTargetNameFromSource(DataPath sourceDataPath, Connection targetConnection) {

    /**
     * By default, we take the logical Name
     */
    DataPath dataPath = targetConnection.getDataPath(sourceDataPath.getLogicalName());

    /**
     * Except if we are on the file system level (physical level)
     */
    if (targetConnection instanceof FsConnection) {

      dataPath = SourceTargetHelperFunction.getTargetDataPathForFileSystem(sourceDataPath, (FsConnection) targetConnection);

    }

    return dataPath;

  }

  public static FsDataPath getTargetDataPathForFileSystem(DataPath sourceDataPath, FsConnection targetConnection) {

    /**
     * FsConnection takes the name and not the logical name as name
     * (ie when we move the file `foo.txt`, to a file system, the name
     * will be `foo.txt`
     */
    String name = sourceDataPath.getName();
    if (sourceDataPath.isScript()) {
      // a query is anonymous and does not have any name
      name = sourceDataPath.getLogicalName();
    }

    if (
      !(sourceDataPath.getConnection() instanceof FsConnection)
        && (sourceDataPath.getOrCreateRelationDef().getColumnsSize() > 0 || sourceDataPath.isScript())
    ) {
      return (FsDataPath) targetConnection
        .getDataPath(name + "." + FS_DEFAULT_TABULAR_MEDIA_TYPE.getExtension(), FS_DEFAULT_TABULAR_MEDIA_TYPE)
        .addVariable("headerRowId", 1);
    }

    return targetConnection.getDataPath(name);

  }


  public SourceTargetHelperFunction setTargetUri(DataUri targetUri) {
    this.targetUri = targetUri;
    return this;
  }


  public SourceTargetHelperFunction setTargetDataDef(Map<String, ?> targetDataDef) {
    this.targetDataDef = targetDataDef;
    return this;
  }
}
