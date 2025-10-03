package com.tabulify.zip.datapath;


import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.zip.api.ArchiveMediaType;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class ArchiveManagerProvider extends FsFileManagerProvider {


  @Override
  public Boolean accept(MediaType mediaType) {


    for (ArchiveMediaType archiveMediaType : ArchiveMediaType.values()) {
      if (MediaTypes.equals(mediaType, archiveMediaType)) {
        return true;
      }
    }
    /**
     * Tar.gz is reported as gzip (gzip is a content compressor, not an archive)
     * but Tar.gz is an archive type
     * <a href="https://superuser.com/questions/901962/what-is-the-correct-mime-type-for-a-tar-gz-file">...</a>
     * This piece of code will not work 100% as the extension is normally gz
     * if it was not created by us.
     * We need to get the path here to correct that
     * As of today, the correction happens:
     * * in the http-fs probe content type that asks for a probe on path extension first
     * * in the {@link com.tabulify.zip.api.ArchiveTypeDetector} for local file system
     */
    for (String extension : ArchiveMediaType.TAR_GZ.getExtensions()) {
      if (mediaType.getExtension().equals(extension)) {
        return true;
      }
    }
    return false;

  }


  @Override
  public FsBinaryFileManager getFsFileManager() {
    return ArchiveManager.archiveManager;
  }


}
