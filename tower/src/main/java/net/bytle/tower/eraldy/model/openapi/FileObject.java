package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A file
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileObject   {


  protected Long localId;

  protected String guid;

  protected String name;

  protected String fullPath;

  protected Integer fileType;

  protected String mediaType;

  protected String thirdType;

  protected FileObject parentFile;

  protected Drive drive;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public FileObject () {
  }

  /**
  * @return localId The file id in the database (unique inside the realm)
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The file id in the database (unique inside the realm)
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return guid The public id (derived from the database/local id)
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public id (derived from the database/local id)
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name The file name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The file name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return fullPath The full path (derived)
  */
  @JsonProperty("fullPath")
  public String getFullPath() {
    return fullPath;
  }

  /**
  * @param fullPath The full path (derived)
  */
  @SuppressWarnings("unused")
  public void setFullPath(String fullPath) {
    this.fullPath = fullPath;
  }

  /**
  * @return fileType The type of file (ie regular file or directory)
  */
  @JsonProperty("fileType")
  public Integer getFileType() {
    return fileType;
  }

  /**
  * @param fileType The type of file (ie regular file or directory)
  */
  @SuppressWarnings("unused")
  public void setFileType(Integer fileType) {
    this.fileType = fileType;
  }

  /**
  * @return mediaType The media type of the content (to parse it)
  */
  @JsonProperty("mediaType")
  public String getMediaType() {
    return mediaType;
  }

  /**
  * @param mediaType The media type of the content (to parse it)
  */
  @SuppressWarnings("unused")
  public void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }

  /**
  * @return thirdType A third functional type for the content For instance: * an xml file can represent a document or a fragment. * a yaml file can be an open api file
  */
  @JsonProperty("thirdType")
  public String getThirdType() {
    return thirdType;
  }

  /**
  * @param thirdType A third functional type for the content For instance: * an xml file can represent a document or a fragment. * a yaml file can be an open api file
  */
  @SuppressWarnings("unused")
  public void setThirdType(String thirdType) {
    this.thirdType = thirdType;
  }

  /**
  * @return parentFile
  */
  @JsonProperty("parentFile")
  public FileObject getParentFile() {
    return parentFile;
  }

  /**
  * @param parentFile Set parentFile
  */
  @SuppressWarnings("unused")
  public void setParentFile(FileObject parentFile) {
    this.parentFile = parentFile;
  }

  /**
  * @return drive
  */
  @JsonProperty("drive")
  public Drive getDrive() {
    return drive;
  }

  /**
  * @param drive Set drive
  */
  @SuppressWarnings("unused")
  public void setDrive(Drive drive) {
    this.drive = drive;
  }

  /**
  * @return creationTime The creation time of the file
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the file
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return modificationTime The last modification time of the file
  */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
  * @param modificationTime The last modification time of the file
  */
  @SuppressWarnings("unused")
  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileObject fileObject = (FileObject) o;
    return

            Objects.equals(localId, fileObject.localId) && Objects.equals(guid, fileObject.guid) && Objects.equals(name, fileObject.name) && Objects.equals(fullPath, fileObject.fullPath) && Objects.equals(fileType, fileObject.fileType) && Objects.equals(mediaType, fileObject.mediaType) && Objects.equals(thirdType, fileObject.thirdType) && Objects.equals(parentFile, fileObject.parentFile) && Objects.equals(drive, fileObject.drive) && Objects.equals(creationTime, fileObject.creationTime) && Objects.equals(modificationTime, fileObject.modificationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId, guid, name, fullPath, fileType, mediaType, thirdType, parentFile, drive, creationTime, modificationTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
