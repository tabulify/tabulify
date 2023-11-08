package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * List creation and/or modification  For creation, the mandatory data are:   * the name   * the publisher app (guid or uri and realm)  If the communication channel should be personal, the publisher (guid / email) can be given.  For modification, you need to give the list guid or the unique handle.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListPostBody   {


  protected String listGuid;

  protected String listHandle;

  protected String listName;

  protected String listTitle;

  protected String listDescription;

  protected String ownerAppGuid;

  protected URI ownerAppUri;

  protected String realmIdentifier;

  protected String ownerUserEmail;

  protected String ownerUserGuid;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListPostBody () {
  }

  /**
  * @return listGuid The public list id, if you want to update the list handle
  */
  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }

  /**
  * @param listGuid The public list id, if you want to update the list handle
  */
  @SuppressWarnings("unused")
  public void setListGuid(String listGuid) {
    this.listGuid = listGuid;
  }

  /**
  * @return listHandle A unique name / code for the list
  */
  @JsonProperty("listHandle")
  public String getListHandle() {
    return listHandle;
  }

  /**
  * @param listHandle A unique name / code for the list
  */
  @SuppressWarnings("unused")
  public void setListHandle(String listHandle) {
    this.listHandle = listHandle;
  }

  /**
  * @return listName The name of the list used in listing
  */
  @JsonProperty("listName")
  public String getListName() {
    return listName;
  }

  /**
  * @param listName The name of the list used in listing
  */
  @SuppressWarnings("unused")
  public void setListName(String listName) {
    this.listName = listName;
  }

  /**
  * @return listTitle The title of the list used in heading/title
  */
  @JsonProperty("listTitle")
  public String getListTitle() {
    return listTitle;
  }

  /**
  * @param listTitle The title of the list used in heading/title
  */
  @SuppressWarnings("unused")
  public void setListTitle(String listTitle) {
    this.listTitle = listTitle;
  }

  /**
  * @return listDescription The description of the list
  */
  @JsonProperty("listDescription")
  public String getListDescription() {
    return listDescription;
  }

  /**
  * @param listDescription The description of the list
  */
  @SuppressWarnings("unused")
  public void setListDescription(String listDescription) {
    this.listDescription = listDescription;
  }

  /**
  * @return ownerAppGuid the owner app guid
  */
  @JsonProperty("ownerAppGuid")
  public String getOwnerAppGuid() {
    return ownerAppGuid;
  }

  /**
  * @param ownerAppGuid the owner app guid
  */
  @SuppressWarnings("unused")
  public void setOwnerAppGuid(String ownerAppGuid) {
    this.ownerAppGuid = ownerAppGuid;
  }

  /**
  * @return ownerAppUri the owner app uri
  */
  @JsonProperty("ownerAppUri")
  public URI getOwnerAppUri() {
    return ownerAppUri;
  }

  /**
  * @param ownerAppUri the owner app uri
  */
  @SuppressWarnings("unused")
  public void setOwnerAppUri(URI ownerAppUri) {
    this.ownerAppUri = ownerAppUri;
  }

  /**
  * @return realmIdentifier the realm identifier (guid or handle), needed if the handle are used, ie user email or app uri
  */
  @JsonProperty("realmIdentifier")
  public String getRealmIdentifier() {
    return realmIdentifier;
  }

  /**
  * @param realmIdentifier the realm identifier (guid or handle), needed if the handle are used, ie user email or app uri
  */
  @SuppressWarnings("unused")
  public void setRealmIdentifier(String realmIdentifier) {
    this.realmIdentifier = realmIdentifier;
  }

  /**
  * @return ownerUserEmail if the communication channel is personal and not transactional, the email of the publisher
  */
  @JsonProperty("ownerUserEmail")
  public String getOwnerUserEmail() {
    return ownerUserEmail;
  }

  /**
  * @param ownerUserEmail if the communication channel is personal and not transactional, the email of the publisher
  */
  @SuppressWarnings("unused")
  public void setOwnerUserEmail(String ownerUserEmail) {
    this.ownerUserEmail = ownerUserEmail;
  }

  /**
  * @return ownerUserGuid if the communication channel is personal and not transactional, the guid of the publisher
  */
  @JsonProperty("ownerUserGuid")
  public String getOwnerUserGuid() {
    return ownerUserGuid;
  }

  /**
  * @param ownerUserGuid if the communication channel is personal and not transactional, the guid of the publisher
  */
  @SuppressWarnings("unused")
  public void setOwnerUserGuid(String ownerUserGuid) {
    this.ownerUserGuid = ownerUserGuid;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListPostBody listPostBody = (ListPostBody) o;
    return Objects.equals(listGuid, listPostBody.listGuid) &&
        Objects.equals(listHandle, listPostBody.listHandle) &&
        Objects.equals(listName, listPostBody.listName) &&
        Objects.equals(listTitle, listPostBody.listTitle) &&
        Objects.equals(listDescription, listPostBody.listDescription) &&
        Objects.equals(ownerAppGuid, listPostBody.ownerAppGuid) &&
        Objects.equals(ownerAppUri, listPostBody.ownerAppUri) &&
        Objects.equals(realmIdentifier, listPostBody.realmIdentifier) &&
        Objects.equals(ownerUserEmail, listPostBody.ownerUserEmail) &&
        Objects.equals(ownerUserGuid, listPostBody.ownerUserGuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(listGuid, listHandle, listName, listTitle, listDescription, ownerAppGuid, ownerAppUri, realmIdentifier, ownerUserEmail, ownerUserGuid);
  }

  @Override
  public String toString() {
    return "class ListPostBody {\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
