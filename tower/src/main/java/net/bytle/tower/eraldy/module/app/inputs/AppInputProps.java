package net.bytle.tower.eraldy.module.app.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.type.Color;
import net.bytle.type.Handle;

import java.net.URI;

/**
 * An app is the container for branding elements (such as logo, color)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppInputProps {



  protected Handle handle;

  protected String name;


  protected URI home;

  protected String slogan;

  protected URI logo;

  protected Color primaryColor;

  protected OrgaUserGuid ownerUserGuid;

  protected URI terms;

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
  * @return home The home URL of the app This is a app branding element that adds an URL to any app communication footer
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
  @JsonProperty("ownerUserGuid")
  public OrgaUserGuid getOwnerUserGuid() {
    return ownerUserGuid;
  }

  /**
  * @param ownerUserGuid Set ownerUser
  */
  @SuppressWarnings("unused")
  public void setOwnerUserGuid(OrgaUserGuid ownerUserGuid) {
    this.ownerUserGuid = ownerUserGuid;
  }


  /**
  * @return terms The location of the terms and conditions document
  */
  @JsonProperty("terms")
  public URI getTermsOfServices() {
    return terms;
  }

  /**
  * @param terms The location of the terms and conditions document
  */
  @SuppressWarnings("unused")
  public void setTerms(URI terms) {
    this.terms = terms;
  }


}
