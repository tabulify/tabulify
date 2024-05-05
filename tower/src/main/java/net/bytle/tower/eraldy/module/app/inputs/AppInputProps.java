package net.bytle.tower.eraldy.module.app.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.type.Color;
import net.bytle.type.Handle;

import java.net.URL;

/**
 * An app is the container for branding elements (such as logo, color)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppInputProps {



  protected Handle handle;

  protected String name;


  protected URL home;

  protected String slogan;

  protected URL logo;

  protected Color primaryColor;

  protected OrgaUserGuid ownerUserGuid;

  protected URL terms;

  private boolean ownerUserGuidSet = false;
  private boolean homeSet = false;
  private boolean handleSet = false;
  private boolean logoSet = false;
  private boolean nameSet = false;
  private boolean termsSet = false;
  private boolean primaryColorSet = false;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AppInputProps() {
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
  @JsonProperty("handle")
  public void setHandle(Handle handle) {
    this.handle = handle;
    this.handleSet = true;
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
    this.nameSet = true;
  }

  /**
  * @return home The home URL of the app This is a app branding element that adds an URL to any app communication footer
  */
  @JsonProperty("home")
  public URL getHome() {
    return home;
  }

  /**
  * @param home The home URL of the app This is a app branding element that adds an URL to any app communication footer
  */
  @JsonProperty("home")
  public void setHome(URL home) {
    this.home = home;
    this.homeSet = true;
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
  public URL getLogo() {
    return logo;
  }

  /**
  * @param logo The uri of the app logo
  */
  @JsonProperty("logo")
  public void setLogo(URL logo) {
    this.logo = logo;
    this.logoSet = true;
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
  @JsonProperty("primaryColor")
  public void setPrimaryColor(Color primaryColor) {
    this.primaryColor = primaryColor;
    this.primaryColorSet = true;
  }

  /**
  * @return ownerUser
  */
  @JsonProperty("ownerUserGuid")
  public OrgaUserGuid getOwnerUserGuid() {
    return ownerUserGuid;
  }

  /**
  * @param ownerUserGuid Set ownerUser
  */
  @SuppressWarnings("ownerUserGuid")
  public void setOwnerUserGuid(OrgaUserGuid ownerUserGuid) {
    this.ownerUserGuid = ownerUserGuid;
    this.ownerUserGuidSet = true;
  }


  /**
  * @return terms The location of the terms and conditions document
  */
  @JsonProperty("terms")
  public URL getTermsOfServices() {
    return terms;
  }

  /**
  * @param terms The location of the terms and conditions document
  */
  @SuppressWarnings("unused")
  public void setTerms(URL terms) {
    this.terms = terms;
    this.termsSet = true;
  }


  public boolean isOwnerUserGuidSet() {
    return ownerUserGuidSet;
  }

  public boolean isHomeSet() {
    return homeSet;
  }

  public boolean isHandleSet() {
    return handleSet;
  }

  public boolean isLogoSet() {
    return logoSet;
  }

  public boolean isNameSet() {
    return nameSet;
  }

  public boolean isTermsSet() {
    return termsSet;
  }

  public boolean isPrimaryColorSet() {
    return primaryColorSet;
  }
}
