package net.bytle.tower.eraldy.module.list.inputs;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List Input Props
 * for the creation and or modification of a list
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListInputProps {

  protected String handle;

  protected String name;

  protected String title;

  private String ownerGuid;
  private Long userCount;
  private Long userInCount;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public ListInputProps() {
  }

  /**
   * @return guid The public id (derived from the database/local id)
   */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
   * @param handle The public id (derived from the database/local id)
   */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }

  /**
   * @return name A short description of the mailing
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param name A short description of the mailing
   */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }


  @JsonProperty("ownerGuid")
  public String getOwnerGuid() {
    return ownerGuid;
  }


  @SuppressWarnings("unused")
  public void setOwnerIdentifier(String ownerGuid) {
    this.ownerGuid = ownerGuid;
  }

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setUserCount(Long userCount) {
    this.userCount = userCount;
  }

  @JsonProperty("userCount")
  public Long getUserCount() {
    return userCount;
  }

  public void setUserInCount(Long userInCount) {
    this.userInCount = userInCount;
  }

  @JsonProperty("userInCount")
  public Long getUserInCount() {
    return userInCount;
  }
}
