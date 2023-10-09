package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The owner of a realm
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealmManager {

  private Realm realm;
  private User owner;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RealmManager() {
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
  * @return owner
  */
  @JsonProperty("owner")
  public User getOwner() {
    return owner;
  }

  /**
  * @param owner Set owner
  */
  @SuppressWarnings("unused")
  public void setOwner(User owner) {
    this.owner = owner;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealmManager realmManager = (RealmManager) o;
    return Objects.equals(realm, realmManager.realm) &&
        Objects.equals(owner, realmManager.owner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(realm, owner);
  }

  @Override
  public String toString() {
    return "class RealmOwner {\n" +
    "    realm: " + toIndentedString(realm) + "\n" +
    "    owner: " + toIndentedString(owner) + "\n" +
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
