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

  protected String path;

  protected Integer fileType;

  protected String mimeType;

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
  * @return path The full path (derived)
  */
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  /**
  * @param path The full path (derived)
  */
  @SuppressWarnings("unused")
  public void setPath(String path) {
    this.path = path;
  }

  /**
  * @return fileType The type of file (regular or directory)
  */
  @JsonProperty("fileType")
  public Integer getFileType() {
    return fileType;
  }

  /**
  * @param fileType The type of file (regular or directory)
  */
  @SuppressWarnings("unused")
  public void setFileType(Integer fileType) {
    this.fileType = fileType;
  }

  /**
  * @return mimeType The mime type of the content (to parse it)
  */
  @JsonProperty("mimeType")
  public String getMimeType() {
    return mimeType;
  }

  /**
  * @param mimeType The mime type of the content (to parse it)
  */
  @SuppressWarnings("unused")
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
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
  * @return creationTime The creation time of the listing
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the listing
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return modificationTime The last modification time of the listing
  */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
  * @param modificationTime The last modification time of the listing
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

            Objects.equals(localId, fileObject.localId) && Objects.equals(guid, fileObject.guid) && Objects.equals(name, fileObject.name) && Objects.equals(path, fileObject.path) && Objects.equals(fileType, fileObject.fileType) && Objects.equals(mimeType, fileObject.mimeType) && Objects.equals(thirdType, fileObject.thirdType) && Objects.equals(parentFile, fileObject.parentFile) && Objects.equals(drive, fileObject.drive) && Objects.equals(creationTime, fileObject.creationTime) && Objects.equals(modificationTime, fileObject.modificationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId, guid, name, path, fileType, mimeType, thirdType, parentFile, drive, creationTime, modificationTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
