package com.tabulify.conf;

import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.fs.Fs;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.yaml.DefaultTimestampWithoutTimeZoneConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A manifest
 * It's more than metadata, it can also describe process such as pipeline
 * It's an object in yaml format
 * Obviously, we borrow the glossary from Kubernetes manifest
 * <p>
 * Why manifest?
 * * Obviously when you open it, you see the kind of object immediately
 * * Versioning
 */
public class ManifestDocument {


  private final MetadataDocumentBuilder builder;
  private KeyNormalizer kind;
  private Map<KeyNormalizer, Object> spec;

  private ManifestDocument(MetadataDocumentBuilder metadataDocumentBuilder) {
    this.builder = metadataDocumentBuilder;
    if (metadataDocumentBuilder.path != null) {
      load(metadataDocumentBuilder.path.toAbsolutePath());
    }
  }

  public static MetadataDocumentBuilder builder() {
    return new MetadataDocumentBuilder();
  }

  private void load(Path manifestPath) {

    // Every document is one file
    List<Map<String, Object>> documents = new ArrayList<>();
    try (InputStream input = Files.newInputStream(manifestPath)) {

      // Transform the file in properties
      Yaml yaml = new Yaml(new DefaultTimestampWithoutTimeZoneConstructor(new LoaderOptions()));

      try {
        for (Object data : yaml.loadAll(input)) {
          Map<String, Object> document;
          try {
            document = Casts.castToSameMap(data, String.class, Object.class);
          } catch (CastException e) {
            String message = "A metadata file must be in a map format. ";
            //noinspection ConstantValue
            if (data.getClass().equals(java.util.ArrayList.class)) {
              message += "They are in a list format. You should suppress the minus if they are present.";
            }
            message += "The Bad Values are: " + data;
            throw new RuntimeException(message, e);
          }
          documents.add(document);
        }
      } catch (Exception e) {
        String message = "Error while parsing the yaml file " + manifestPath + ".";
        if (e instanceof ScannerException) {
          message += " Scanner ";
        }
        if (e instanceof ParserException) {
          message += " Parser ";
        }
        throw new RuntimeException(message + " Error: \n" + e.getMessage());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error while trying to read the metadata file (" + manifestPath + "). Error: " + e.getMessage(), e);
    }

    switch (documents.size()) {
      case 0:
        throw new RuntimeException("No data was found in the metadata file (" + manifestPath + "). The file seems to be empty.");
      case 1:

        Map<KeyNormalizer, Object> document;
        try {
          document = Casts.castToNewMap(documents.get(0), KeyNormalizer.class, Object.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The document of the file (" + manifestPath + ") is not a valid map.  Error " + e.getMessage(), e);
        }
        for (Map.Entry<KeyNormalizer, Object> metadataEntry : document.entrySet()) {
          ManifestAttribute manifestAttribute;
          KeyNormalizer key = metadataEntry.getKey();
          try {
            manifestAttribute = Casts.cast(key, ManifestAttribute.class);
          } catch (CastException e) {
            /**
             * On one line so that we see the full error on maven
             */
            throw new IllegalArgumentException("The property (" + key + ") is not a valid metadata property for the metadata file. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ManifestAttribute.class) + ". Metadata file: (" + this + ").");
          }
          switch (manifestAttribute) {
            case KIND:
              KeyNormalizer kind;
              Object kindValue = metadataEntry.getValue();
              try {
                kind = KeyNormalizer.create(kindValue);
              } catch (CastException e) {
                throw new IllegalArgumentException("The kind value (" + kindValue + ") is not a valid name. Error: " + e.getMessage(), e);
              }
              this.setKind(kind);
              break;
            case SPEC:
              Map<KeyNormalizer, Object> kvMap;
              try {
                kvMap = Casts.castToNewMap(metadataEntry.getValue(), KeyNormalizer.class, Object.class);
              } catch (CastException e) {
                throw new IllegalArgumentException("The metadata property has a value that is not a map. Metadata File (" + manifestPath + ") Error: " + e.getMessage(), e);
              }

              this.setMetadataMap(kvMap);
              break;
            default:
              throw new InternalException("The metadata attribute (" + manifestAttribute + ") is not in the switch branch)");

          }
        }

        break;
      default:
        throw new IllegalArgumentException("Too much yaml documents (" + documents.size() + ") found in the manifest (" + this + ")");
    }
    /**
     * Check
     */
    if (spec == null) {
      throw new IllegalArgumentException("the spec property was not found in the metadata file (" + this + ")");
    }
    if (spec.isEmpty()) {
      throw new IllegalArgumentException("the spec map is empty for the metadata file (" + this + ")");
    }
    if (kind == null) {
      throw new IllegalArgumentException("the kind property was not found in the metadata file (" + this + ")");
    }

    if (builder.expectedKind != null) {
      if (!kind.equals(builder.expectedKind)) {
        throw new IllegalArgumentException("The kind does not match. The manifest (" + manifestPath + ") has a kind of " + kind + " but the expected one is " + builder.expectedKind);
      }
    }

  }

  private void setMetadataMap(Map<KeyNormalizer, Object> metadata) {
    this.spec = metadata;
  }

  private void setKind(KeyNormalizer kind) {
    this.kind = kind;
  }

  public KeyNormalizer getKind() {
    return this.kind;
  }

  public Map<KeyNormalizer, Object> getSpecMap() {
    return this.spec;
  }

  public Path getPath() {
    return this.builder.path;
  }

  public static class MetadataDocumentBuilder {

    private Path path;
    private KeyNormalizer expectedKind;

    public MetadataDocumentBuilder setPath(Path path) {
      this.path = path;
      return this;
    }

    public ManifestDocument build() {
      if (!Files.exists(path))
        throw new IllegalArgumentException("The manifest file path (" + path.toAbsolutePath() + " does not exist");
      if (!Files.isRegularFile(path))
        throw new IllegalArgumentException("The manifest file path (" + path.toAbsolutePath() + " is not a regular file");

      List<String> yamlExtensions = Arrays.asList("yml", "yaml");
      if (!yamlExtensions.contains(Fs.getExtension(path))) {

        throw new IllegalArgumentException("The manifest file (" + path + ") should be a yaml file with the extension (" + String.join(", ", yamlExtensions) + " )");

      }
      return new ManifestDocument(this);
    }

    public MetadataDocumentBuilder setExpectedKind(KeyNormalizer expectedKind) {
      this.expectedKind = expectedKind;
      return this;
    }
  }

  @Override
  public String toString() {
    String kindString = kind != null ? " (" + kind + ")" : "";
    return Objects.requireNonNullElse(this.builder.path, "anonymous metadata") + kindString;
  }
}
