package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.realm.model.Realm;

import java.util.Objects;

/**
 * Service
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceUser   {

  private Long id;
  private String guid;
  private Service service;
  private User user;
  private Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ServiceUser () {
  }

  /**
  * @return id The service scope id in the realm
  */
  @JsonProperty("id")
  public Long getLocalId() {
    return id;
  }

  /**
  * @param id The service scope id in the realm
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long id) {
    this.id = id;
  }

  /**
  * @return guid The service scope global id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The service scope global id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return service
  */
  @JsonProperty("service")
  public Service getService() {
    return service;
  }

  /**
  * @param service Set service
  */
  @SuppressWarnings("unused")
  public void setService(Service service) {
    this.service = service;
  }

  /**
  * @return user
  */
  @JsonProperty("user")
  public User getUser() {
    return user;
  }

  /**
  * @param user Set user
  */
  @SuppressWarnings("unused")
  public void setUser(User user) {
    this.user = user;
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
    ServiceUser serviceUser = (ServiceUser) o;
    return Objects.equals(id, serviceUser.id) &&
        Objects.equals(guid, serviceUser.guid) &&
        Objects.equals(service, serviceUser.service) &&
        Objects.equals(user, serviceUser.user) &&
        Objects.equals(realm, serviceUser.realm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, service, user, realm);
  }

  @Override
  public String toString() {
    return "class ServiceUser {\n" +
    "}";
  }


}
