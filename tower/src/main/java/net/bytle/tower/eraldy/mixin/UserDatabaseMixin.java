package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.Realm;

import java.time.LocalDateTime;


/**
 * The database mixin to store the user
 * <p>
 * Remove the data that are already in the row
 * We keep the guid for backup :)
 *
 */
public abstract class UserDatabaseMixin {

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
  @JsonProperty("realm")
  abstract Realm getRealm();

  @JsonIgnore
  @JsonProperty("creationTime")
  abstract LocalDateTime getCreationTime();

  @JsonIgnore
  @JsonProperty("modificationTime")
  abstract LocalDateTime getModificationTime();

}
