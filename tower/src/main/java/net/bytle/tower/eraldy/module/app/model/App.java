package net.bytle.tower.eraldy.module.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.type.Color;
import net.bytle.type.Handle;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An app is the container for branding elements (such as logo, color)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class App {

  protected AppGuid guid;

  protected Handle handle;

  protected String name;

  protected URI uri;

  protected URI home;

  protected String slogan;

  protected URI logo;

  protected Color primaryColor;

  protected OrgaUser ownerUser;

  protected Realm realm;

  protected URI terms;
  private LocalDateTime creationTime;
  private LocalDateTime modificationTime;
  private Integer listCount;

  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public App() {
  }

  /**
   * @return guid The global app id (realm id + local app id)
   */
  @JsonProperty("guid")
  public AppGuid getGuid() {
    return guid;
  }

  /**
   * @param guid The global app id (realm id + local app id)
   */
  @SuppressWarnings("unused")
  public void setGuid(AppGuid guid) {
    this.guid = guid;
  }

  /**
   * @return handle The handle of the app. The handle is unique for all apps on the realm. It follows the DNS name constraint
   */
  @JsonProperty("handle")
  public Handle getHandle() {
    return handle;
  }

  /**
   * @param handle The handle of the app. The handle is unique for all apps on the realm. It follows the DNS name constraint
   */
  @SuppressWarnings("unused")
  public void setHandle(Handle handle) {
    this.handle = handle;
  }

  /**
   * @return name The name of the app
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param name The name of the app
   */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

    /**
   * @return home The home URL of the app This is an app branding element that adds an URL to any app communication footer
   */
  @JsonProperty("home")
  public URI getHome() {
    return home;
  }

  /**
   * @param home The home URL of the app This is a app branding element that adds an URL to any app communication footer
   */
  @SuppressWarnings("unused")
  public void setHome(URI home) {
    this.home = home;
  }

  /**
   * @return slogan The slogan of the app
   */
  @JsonProperty("slogan")
  public String getSlogan() {
    return slogan;
  }

  /**
   * @param slogan The slogan of the app
   */
  @SuppressWarnings("unused")
  public void setSlogan(String slogan) {
    this.slogan = slogan;
  }

  /**
   * @return logo The uri of the app logo
   */
  @JsonProperty("logo")
  public URI getLogo() {
    return logo;
  }

  /**
   * @param logo The uri of the app logo
   */
  @SuppressWarnings("unused")
  public void setLogo(URI logo) {
    this.logo = logo;
  }

  /**
   * @return primaryColor The css primary color of the theme (rgb in hexadecimal)
   */
  @JsonProperty("primaryColor")
  public Color getPrimaryColor() {
    return primaryColor;
  }

  /**
   * @param primaryColor The css primary color of the theme (rgb in hexadecimal)
   */
  @SuppressWarnings("unused")
  public void setPrimaryColor(Color primaryColor) {
    this.primaryColor = primaryColor;
  }

  /**
   * @return ownerUser
   */
  @JsonProperty("ownerUser")
  public OrgaUser getOwnerUser() {
    return ownerUser;
  }

  /**
   * @param ownerUser Set ownerUser
   */
  @SuppressWarnings("unused")
  public void setOwnerUser(OrgaUser ownerUser) {
    this.ownerUser = ownerUser;
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

  /**
   * @return terms The location of the terms and conditions document
   */
  @JsonProperty("tos")
  public URI getTermsOfServices() {
    return terms;
  }

  /**
   * @param terms The location of the terms and conditions document
   */
  @SuppressWarnings("unused")
  public void setTermsOfServices(URI terms) {
    this.terms = terms;
  }

  public void setCreationTime(LocalDateTime localDateTime) {
    this.creationTime = localDateTime;
  }

  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return this.creationTime;
  }

  public void setModificationTime(LocalDateTime localDateTime) {
    this.modificationTime = localDateTime;
  }

  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return this.modificationTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    App app = (App) o;
    return Objects.equals(guid, app.guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid + ", " + Objects.requireNonNullElse(handle, name);
  }

  public void setListCount(Integer listCount) {
    this.listCount = listCount;
  }

  @JsonProperty("listCount")
  public Integer getListCount() {
    return listCount;
  }

}
