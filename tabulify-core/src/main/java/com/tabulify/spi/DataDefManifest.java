package com.tabulify.spi;

import com.tabulify.conf.ManifestDocument;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.dir.FsDirectoryDataPath;
import com.tabulify.resource.ManifestKindMediaType;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DataDefManifest {
  public static final MediaType DATA_DEF_MEDIA_TYPE = new ManifestKindMediaType(KeyNormalizer.createSafe("data-def"));


  public static void mergeDataDef(DataPath dataPath, MediaType mediaType) {
    if (dataPath instanceof FsDirectoryDataPath) {
      return;
    }
    if (dataPath.isRuntime()) {
      return;
    }
    if (!(dataPath instanceof FsDataPath)) {
      // manifest can return SQL Data Path
      return;
    }
    /**
     * Only on the local file system (ie file)
     * not on network for performance reason.
     * With HTTP, it can add up really quickly
     */
    Path absolutePath = ((FsDataPath) dataPath).getAbsoluteNioPath();
    if (!absolutePath.toUri().getScheme().equals("file")) {
      return;
    }

    /**
     * Data Def from a kind manifest
     */
    Path yamlKind = absolutePath.resolveSibling(dataPath.getLogicalName() + "--" + mediaType.getKind().toKebabCase() + ".yml");
    if (Files.exists(yamlKind)) {
      Map<KeyNormalizer, Object> spec = ManifestDocument.builder()
        .setPath(yamlKind)
        .setExpectedKind(mediaType.getKind())
        .build().getSpecMap();
      Object dataDefObject = spec.get(KeyNormalizer.createSafe("data-def"));
      if (dataDefObject == null) {
        return;
      }
      Map<KeyNormalizer, Object> dataDefMap;
      try {
        dataDefMap = Casts.castToNewMap(dataDefObject, KeyNormalizer.class, Object.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The data-def property has a value that is not a map. Metadata File (" + yamlKind + ") Error: " + e.getMessage(), e);
      }
      dataPath.mergeDataDefinitionFromYamlMap(dataDefMap);
      return;
    }

    /**
     * Data Def from a data def manifest
     */
    Path yamlDataDef = absolutePath.resolveSibling(dataPath.getLogicalName() + DataDefManifest.DATA_DEF_MEDIA_TYPE.getExtension());
    if (!Files.exists(yamlDataDef)) {
      return;
    }

    ManifestDocument manifest = ManifestDocument.builder()
      .setPath(yamlDataDef)
      .setExpectedKind(DataDefManifest.DATA_DEF_MEDIA_TYPE.getKind())
      .build();
    dataPath.mergeDataDefinitionFromYamlMap(manifest.getSpecMap());


  }


}
