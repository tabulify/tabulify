package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A realm with the app Uri that it supports 
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealmWithAppUris   {


  protected String guid;

  protected String name;

  protected String handle;

  protected List<URI> appUris;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RealmWithAppUris () {
  }

  /**
  * @return guid The public id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name A name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return handle the handle
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle the handle
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }

  /**
  * @return appUris
  */
  @JsonProperty("app_uris")
  public List<URI> getAppUris() {
    return appUris;
  }

  /**
  * @param appUris Set appUris
  */
  @SuppressWarnings("unused")
  public void setAppUris(List<URI> appUris) {
    this.appUris = appUris;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealmWithAppUris realmWithAppUris = (RealmWithAppUris) o;
    return 
            
            Objects.equals(guid, realmWithAppUris.guid) && Objects.equals(name, realmWithAppUris.name) && Objects.equals(handle, realmWithAppUris.handle) && Objects.equals(appUris, realmWithAppUris.appUris);
  }

  @Override
  public int hashCode() { 
    return Objects.hash(guid, name, handle, appUris);
  }

  @Override 
  public String toString() {
    return super.toString();
  }

}
