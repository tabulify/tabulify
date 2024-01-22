package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;


/**
 * A mixin to create a JSON to be stored in the database.
 * It permits to avoid recursion during serialization   as a Realm is also a property of a User.
 * <p>
 * The ignored field are columns present in the row.
 */
public abstract class RealmDatabaseMixin {

  // Because Guid is part of the object identity
  // we can't ignore it
  // otherwise we get
  // com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Invalid Object Id definition, cannot find property with name `guid`
  @JsonProperty("guid")
  abstract String getGuid();

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();

  @JsonIgnore
  @JsonProperty("handle")
  abstract String getHandle();

  @JsonIgnore
  @JsonProperty("ownerUser")
  abstract OrganizationUser getOwnerUser();

  @JsonIgnore
  @JsonProperty("organization")
  abstract Organization getOrganization();

  @JsonIgnore
  @JsonProperty("defaultApp")
  abstract App getDefaultApp();

}
