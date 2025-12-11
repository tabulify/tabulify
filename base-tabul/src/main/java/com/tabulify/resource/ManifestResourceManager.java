package com.tabulify.resource;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.ManifestDocument;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManager;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.ExceptionWrapper;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.resource.ManifestResourceAttribute.DATA_URI;
import static com.tabulify.resource.ManifestResourceProvider.MEDIA_TYPE;

public class ManifestResourceManager implements FsFileManager {


  @Override
  public DataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    Path manifestPath = fsConnection.getCurrentDataPath().getAbsoluteNioPath().resolve(relativePath);

    try {

      ManifestDocument manifest = ManifestDocument.builder()
        .setPath(manifestPath)
        .build();
      if (!manifest.getKind().equals(MEDIA_TYPE.getKind())) {
        throw new IllegalArgumentException("The kind does not match its media type. The manifest has a kind of " + manifest.getKind() + " and should be a " + MEDIA_TYPE.getKind());
      }

      DataUriStringNode dataUriString = null;
      Map<KeyNormalizer, Object> dataDef = Map.of();
      MediaType targetMediaType = null;
      for (Map.Entry<KeyNormalizer, Object> entry : manifest.getSpecMap().entrySet()) {
        ManifestResourceAttribute manifestResourceAttribute;
        try {
          manifestResourceAttribute = Casts.cast(entry.getKey(), ManifestResourceAttribute.class);
        } catch (CastException e) {
          String expectedKeyAsString = Arrays.stream(ManifestResourceAttribute.class.getEnumConstants())
            .sorted()
            .map(c -> KeyNormalizer.createSafe(c).toCliLongOptionName())
            .collect(Collectors.joining(", "));
          throw new IllegalArgumentException("The attribute (" + entry.getKey() + ") is not a resource attribute. We were expecting one of " + expectedKeyAsString);
        }
        Attribute attribute;
        Object value = entry.getValue();
        try {
          attribute = fsConnection.getTabular().getVault()
            .createVariableBuilderFromAttribute(manifestResourceAttribute)
            .setOrigin(Origin.MANIFEST)
            .build(value);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + manifestResourceAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
        }


        switch (manifestResourceAttribute) {
          case DATA_URI:
            dataUriString = (DataUriStringNode) attribute.getValueOrDefault();
            break;
          case DATA_DEF:
            try {
              dataDef = Casts.castToNewMap(attribute.getValueOrDefault(), KeyNormalizer.class, Object.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The data def attribute (" + manifestResourceAttribute + ") is not valid. Error: " + e.getMessage(), e);
            }
            break;
          case MEDIA_TYPE:
            targetMediaType = (MediaType) attribute.getValueOrDefault();
            break;
          default:
            throw new InternalException("The attribute (" + manifestResourceAttribute + ") is missing in the switch branch");

        }

      }

      if (dataUriString == null) {
        throw new IllegalArgumentException("No " + DATA_URI + " attribute found in the resource attribute manifest (" + manifest + ")");
      }
      DataUriNode dataUri = DataUriBuilder
        .builder(fsConnection.getTabular())
        .addManifestDirectoryConnection(manifestPath.getParent())
        .build()
        .apply(dataUriString);

      return fsConnection.getTabular().getDataPath(dataUri, targetMediaType)
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


}
