package net.bytle.tower.eraldy.mixin;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An app is the container for branding elements (such as logo, color)
 **/
public abstract class AppPublicMixinWithRealm {

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();


}
