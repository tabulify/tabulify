package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Organization;


/**
 *
 * You can not change mix-ins on-the-fly
 *
 * Mix-ins are useful for applying Jackson configuration to classes
 * without modifying the source code of the target class.
 * Mix-ins can be thought of as a configuration layer that sits above a class
 * that Jackson will look at for instructions during de/serialization.
 * <a href="https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations">...</a>
 *
 */
public abstract class RealmPublicMixin {


  @JsonIgnore
  @JsonProperty("defaultApp")
  abstract App getDefaultApp();

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();

  @JsonSerialize(as = Organization.class)
  @JsonProperty("organization")
  abstract Organization getOrganization();

}
