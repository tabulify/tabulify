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
