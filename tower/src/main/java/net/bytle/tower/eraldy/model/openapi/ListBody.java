package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List creation and update  If the communication channel should be personal, the publisher (guid / email) can be given. 
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListBody   {


  protected String listHandle;

  protected String listName;

  protected String listTitle;

  protected String listDescription;

  protected String ownerAppIdentifier;

  protected String ownerUserIdentifier;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListBody () {
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
  * @return ownerAppIdentifier A owner app identifier (handle or guid)
  */
  @JsonProperty("ownerAppIdentifier")
  public String getOwnerAppIdentifier() {
    return ownerAppIdentifier;
  }

  /**
  * @param ownerAppIdentifier A owner app identifier (handle or guid)
  */
  @SuppressWarnings("unused")
  public void setOwnerAppIdentifier(String ownerAppIdentifier) {
    this.ownerAppIdentifier = ownerAppIdentifier;
  }

  /**
  * @return ownerUserIdentifier if the communication channel is personal and not transactional, a user identifier (email or guid) of the publisher
  */
  @JsonProperty("ownerUserIdentifier")
  public String getOwnerUserIdentifier() {
    return ownerUserIdentifier;
  }

  /**
  * @param ownerUserIdentifier if the communication channel is personal and not transactional, a user identifier (email or guid) of the publisher
  */
  @SuppressWarnings("unused")
  public void setOwnerUserIdentifier(String ownerUserIdentifier) {
    this.ownerUserIdentifier = ownerUserIdentifier;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListBody listBody = (ListBody) o;
    return Objects.equals(listHandle, listBody.listHandle);
  }

  @Override
  public int hashCode() { 
    return Objects.hash(listHandle);
  }

  @Override 
  public String toString() {
    return listHandle;
  }

}
