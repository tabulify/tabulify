package net.bytle.tower.eraldy.module.realm.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.type.Handle;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealmInputProps {



  protected String name;

  protected Handle handle;
  private Long appCount;
  private Long userCount;
  private Long listCount;
  private OrgaUserGuid ownerUserGuid;


  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RealmInputProps() {
  }

  /**
  * @return name A short description of the realm
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A short description of the realm
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }


  /**
  * @return handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
  */
  @JsonProperty("handle")
  public Handle getHandle() {
    return handle;
  }

  /**
  * @param handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
  */
  @SuppressWarnings("unused")
  public void setHandle(Handle handle) {
    this.handle = handle;
  }

  /**
  * @return userCount The number of users for the realm
  */
  @JsonProperty("userCount")
  public Long getUserCount() {
    return userCount;
  }

  /**
  * @param userCount The number of users for the realm
  */
  @SuppressWarnings("unused")
  public void setUserCount(Long userCount) {
    this.userCount = userCount;
  }

  /**
  * @return appCount The number of apps for the realm
  */
  @JsonProperty("appCount")
  public Long getAppCount() {
    return appCount;
  }

  /**
  * @param appCount The number of apps for the realm
  */
  @SuppressWarnings("unused")
  public void setAppCount(Long appCount) {
    this.appCount = appCount;
  }

  /**
  * @return listCount The number of lists for the realm
  */
  @JsonProperty("listCount")
  public Long getListCount() {
    return listCount;
  }

  /**
  * @param listCount The number of lists for the realm
  */
  @SuppressWarnings("unused")
  public void setListCount(Long listCount) {
    this.listCount = listCount;
  }


  public OrgaUserGuid getOwnerUserGuid() {
    return this.ownerUserGuid;
  }

  public void setOwnerUserGuid(OrgaUserGuid ownerUserGuid) {
    this.ownerUserGuid = ownerUserGuid;
  }

}
