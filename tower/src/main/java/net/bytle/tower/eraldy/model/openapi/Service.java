package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Service
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Service   {


  protected Long id;

  protected String guid;

  protected String uri;

  protected String type;

  protected Object data;

  protected User impersonatedUser;

  protected Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Service () {
  }

  /**
  * @return id The service id in the realm
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return id;
  }

  /**
  * @param id The service id in the realm
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long id) {
    this.id = id;
  }

  /**
  * @return guid The service global id, if you want to update the uri identifier
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The service global id, if you want to update the uri identifier
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return uri An unique identifier (uri like) for the service
  */
  @JsonProperty("uri")
  public String getUri() {
    return uri;
  }

  /**
  * @param uri An unique identifier (uri like) for the service
  */
  @SuppressWarnings("unused")
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
  * @return type The type of the service (smtp, ...) that define the data type
  */
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  /**
  * @param type The type of the service (smtp, ...) that define the data type
  */
  @SuppressWarnings("unused")
  public void setType(String type) {
    this.type = type;
  }

  /**
  * @return data The configuration for the service
  */
  @JsonProperty("data")
  public Object getData() {
    return data;
  }

  /**
  * @param data The configuration for the service
  */
  @SuppressWarnings("unused")
  public void setData(Object data) {
    this.data = data;
  }

  /**
  * @return impersonatedUser
  */
  @JsonProperty("impersonatedUser")
  public User getImpersonatedUser() {
    return impersonatedUser;
  }

  /**
  * @param impersonatedUser Set impersonatedUser
  */
  @SuppressWarnings("unused")
  public void setImpersonatedUser(User impersonatedUser) {
    this.impersonatedUser = impersonatedUser;
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
    Service service = (Service) o;
    return
            Objects.equals(guid, service.guid);

  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid;
  }

}
