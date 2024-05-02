package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.realm.model.Realm;

import java.util.Objects;

/**
 * Service
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceApp   {

  private Long id;
  private String guid;
  private Service service;
  private App app;
  private Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ServiceApp () {
  }

  /**
  * @return id The service scope id in the realm
  */
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  /**
  * @param id The service scope id in the realm
  */
  @SuppressWarnings("unused")
  public void setId(Long id) {
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
    ServiceApp serviceApp = (ServiceApp) o;
    return Objects.equals(id, serviceApp.id) &&
        Objects.equals(guid, serviceApp.guid) &&
        Objects.equals(service, serviceApp.service) &&
        Objects.equals(app, serviceApp.app) &&
        Objects.equals(realm, serviceApp.realm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, service, app, realm);
  }

  @Override
  public String toString() {
    return "class ServiceApp {\n" +
    "}";
  }

}
