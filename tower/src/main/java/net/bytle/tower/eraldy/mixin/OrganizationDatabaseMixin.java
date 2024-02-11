package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * In this mixin, we ignore the data that are
 * in the table
 */
public abstract class OrganizationDatabaseMixin {

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();

  @JsonIgnore
  @JsonProperty("guid")
  abstract String getGuid();

  @JsonProperty("handle")
  abstract String getHandle();

  @JsonProperty("creationTime")
  abstract LocalDateTime getCreationTime();

  @JsonProperty("modificationTime")
  abstract LocalDateTime getModificationTime();

}
