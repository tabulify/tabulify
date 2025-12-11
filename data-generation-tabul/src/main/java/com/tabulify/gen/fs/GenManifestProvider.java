package com.tabulify.gen.fs;

import com.tabulify.conf.ManifestDocument;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.gen.DataGenerator;
import com.tabulify.gen.GenDataPath;
import com.tabulify.gen.GeneratorMediaType;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.tabulify.gen.GeneratorMediaType.FILE_NAME_SUFFIX;

public class GenManifestProvider extends FsFileManagerProvider {


  static final GenManifestManager GEN_MANIFEST_MANAGER = new GenManifestManager();

  @Override
  public Boolean accept(MediaType mediaType) {

    if (mediaType.equals(GeneratorMediaType.FS_GENERATOR_TYPE)) {
      return true;
    }
    return mediaType.equals(GeneratorMediaType.FS_INLINE_GENERATOR_TYPE);

  }


  @Override
  public GenManifestManager getFsFileManager() {
    return GEN_MANIFEST_MANAGER;
  }


  public static class GenManifestManager implements FsFileManager {


    /**
     * We return a GenMemData Path so that by default,
     * when transferring it, a copy will not see a file but a memory resource.
     * If it was seeing a file, it would have copied the manifest file content and not the data generated
     */
    @Override
    public GenDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

      String name = relativePath.getFileName().toString().replace(FILE_NAME_SUFFIX, "");

      GenDataPath dataGen = DataGenerator
        .create(fsConnection.getTabular())
        .createGenDataPath(name);

      /**
       * Fragment
       */
      if (mediaType.equals(GeneratorMediaType.FS_INLINE_GENERATOR_TYPE)) {
        return dataGen;
      }

      /**
       * File
       */
      Path absoluteNioPath = fsConnection.getDataSystem().toAbsolutePath(relativePath);
      if (!Files.exists(absoluteNioPath)) {
        throw new IllegalArgumentException("The generator (" + absoluteNioPath + ") does not exists");
      }
      ManifestDocument manifest = ManifestDocument.builder().setPath(absoluteNioPath).build();
      dataGen.setManifest(manifest);
      KeyNormalizer kind = manifest.getKind();
      if (!kind.equals(GeneratorMediaType.KIND)) {
        throw new IllegalArgumentException("The metadata is not a " + GeneratorMediaType.KIND + " kind but a " + kind);
      }
      dataGen.mergeDataDefinitionFromYamlMap(manifest.getSpecMap());

      return dataGen;

    }

    @Override
    public void create(FsDataPath fsDataPath) {
      throw new UnsupportedOperationException("Generator cannot be created");
    }

    @Override
    public FsDataPath createRuntimeDataPath(FsConnection executionConnection, FsDataPath executableDataPath) {
      return FsFileManager.super.createRuntimeDataPath(executionConnection, executableDataPath);
    }


  }


}
