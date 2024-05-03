package net.bytle.tower.eraldy.model.manual;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.realm.model.Realm;

import java.util.Objects;

/**
 * A drive for a file is a container
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDrive {


  protected Long localId;

  protected String guid;

  protected String name;

  protected App app;

  protected Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public FileDrive() {
  }

  /**
  * @return localId The drive id in the database
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The drive id in the database
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
  * @return name A short description of the drive
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A short description of the drive
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return app
  */
  @JsonProperty("app")
  public App getApp() {
    return app;
  }

  /**
  * @param app Set app
  */
  @SuppressWarnings("unused")
  public void setApp(App app) {
    this.app = app;
  }

  /**
  * @return realm
  */
  @JsonProperty("realm")
  public Realm getRealm() {
    return realm;
  }

  /**
  * @param realm Set realm
  */
  @SuppressWarnings("unused")
  public void setRealm(Realm realm) {
    this.realm = realm;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileDrive fileDrive = (FileDrive) o;
    return

            Objects.equals(localId, fileDrive.localId) && Objects.equals(guid, fileDrive.guid) && Objects.equals(name, fileDrive.name) && Objects.equals(app, fileDrive.app) && Objects.equals(realm, fileDrive.realm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId, guid, name, app, realm);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
