package com.tabulify.resource;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.ManifestDocument;
import com.tabulify.conf.Origin;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.exception.CastException;
import net.bytle.exception.ExceptionWrapper;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.resource.ManifestKindMediaType.MANIFEST_SEPARATION;

/**
 * Create a data path from a manifest
 * where the media-type is taken from the kind attribute
 * The plugin would normally build it as a manager for {@link FsFileManagerProvider#getFsFileManager()}
 * with the target media type
 * For an example, see SqlQueryManager
 */
public class ManifestKindManager implements FsFileManager {

  private final MediaType targetMediaType;

  private ManifestKindManager(ResourceKindManagerBuilder resourceKindManagerBuilder) {

    this.targetMediaType = resourceKindManagerBuilder.targetMediaType;

  }

  @Override
  public DataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {


    Path manifestPath = fsConnection.getCurrentDataPath().getAbsoluteNioPath().resolve(relativePath);

    /**
     * If it does not exist, we don't throw
     * as data path is a meta holder structure
     */
    if (!Files.exists(manifestPath)) {
      return new FsTextDataPath(fsConnection, relativePath);
    }
    try {

      ManifestDocument manifest = ManifestDocument.builder()
        .setPath(manifestPath)
        .setExpectedKind(this.targetMediaType.getKind())
        .build();

      DataUriStringNode dataUriStringNode = null;
      Map<KeyNormalizer, Object> dataDef = Map.of();

      for (Map.Entry<KeyNormalizer, Object> entry : manifest.getSpecMap().entrySet()) {
        ManifestKindAttribute resourceAttribute;
        try {
          resourceAttribute = Casts.cast(entry.getKey(), ManifestKindAttribute.class);
        } catch (CastException e) {
          String expectedKeyAsString = Arrays.stream(ManifestKindAttribute.class.getEnumConstants())
            .sorted()
            .map(c -> KeyNormalizer.createSafe(c).toCliLongOptionName())
            .collect(Collectors.joining(", "));
          throw new IllegalArgumentException("The attribute (" + entry.getKey() + ") is not a resource attribute. We were expecting one of: " + expectedKeyAsString, e);
        }
        Attribute attribute;
        Object value = entry.getValue();
        try {
          attribute = fsConnection.getTabular().getVault()
            .createVariableBuilderFromAttribute(resourceAttribute)
            .setOrigin(Origin.MANIFEST)
            .build(value);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + resourceAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
        }


        switch (resourceAttribute) {
          case DATA_URI:
            dataUriStringNode = (DataUriStringNode) attribute.getValueOrDefault();
            break;
          case DATA_DEF:
            try {
              dataDef = Casts.castToNewMap(attribute.getValueOrDefault(), KeyNormalizer.class, Object.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The data def attribute (" + resourceAttribute + ") is not valid. Error: " + e.getMessage(), e);
            }
            break;
          default:
            throw new InternalException("The attribute (" + resourceAttribute + ") is missing in the switch branch");

        }

      }

      if (dataUriStringNode == null) {
        /**
         * For a runtime resource, the data uri is required.
         * As it's a virtual resource, we can't derive a runtime data uri
         * as the connection would still be unknown,
         * and they have no file extension
         */
        if (this.targetMediaType.isRuntime()) {
          throw new InternalException("A data uri is mandatory for a runtime resource. No data uri was not found for the resource " + targetMediaType + " .");
        }
        String fileName = Fs.getFileNameWithoutExtension(manifestPath).replace(MANIFEST_SEPARATION + this.targetMediaType.getExtension(), "");
        dataUriStringNode = DataUriStringNode.builder()
          .setConnection(ConnectionBuiltIn.MD_LOCAL_FILE_SYSTEM)
          .setPath(fileName + "." + this.targetMediaType.getExtension())
          .build();
      }
      /**
       * For a runtime resource, the data uri path should be a runtime uri
       */
      if (this.targetMediaType.isRuntime() && dataUriStringNode.getPathNode() == null) {
        throw new IllegalArgumentException("For a runtime resource of type (" + targetMediaType + "), the data uri should be a runtime data uri defining an executable. The data uri (" + dataUriStringNode + ") is static uri, not a runtime uri");
      }
      /**
       * For a static resource, the data uri path should not be a runtime uri
       */
      if (!this.targetMediaType.isRuntime() && dataUriStringNode.getPathNode() != null) {
        throw new IllegalArgumentException("For a static resource of type (" + targetMediaType + "), the data uri should be a static uri, not a runtime uri. The value (" + dataUriStringNode + ") is a runtime uri.");
      }

      DataUriNode dataUri = DataUriBuilder
        .builder(fsConnection.getTabular())
        .addManifestDirectoryConnection(manifestPath.getParent())
        .build()
        .apply(dataUriStringNode);

      // A data uri with a manifest path?
      if (dataUriStringNode.getPath() != null && dataUriStringNode.getPath().contains("--")) {
        DataPath dataPath = fsConnection
          .getTabular()
          .getDataPath(dataUri, null);
        if (dataPath.isRuntime()) {
          dataUri = dataPath.execute().toDataUri();
        } else {
          /**
           * If the dataUriStringNode path is not a manifest, the data uri value
           * should be the same as the original dataUri
           */
          dataUri = dataPath.toDataUri();
        }
      }

      return fsConnection
        .getTabular()
        .getDataPath(dataUri, targetMediaType)
        .mergeDataDefinitionFromYamlMap(dataDef);
    } catch (IllegalArgumentException | InternalException e) {
      throw ExceptionWrapper.builder(e, "The manifest file returns an error. Manifest: (" + manifestPath + ")")
        .setPosition(ExceptionWrapper.ContextPosition.FIRST)
        .buildAsRuntimeException();
    }

  }

  @Override
  public void create(FsDataPath fsDataPath) {
    throw new UnsupportedOperationException("Resource manifest cannot be created");
  }

  public static ResourceKindManagerBuilder builder() {
    return new ResourceKindManagerBuilder();
  }

  public static class ResourceKindManagerBuilder {


    private MediaType targetMediaType;

    public ResourceKindManagerBuilder setTargetMediaType(MediaType targetMediaType) {
      this.targetMediaType = targetMediaType;
      return this;
    }

    public ManifestKindManager build() {
      return new ManifestKindManager(this);
    }

  }
}
