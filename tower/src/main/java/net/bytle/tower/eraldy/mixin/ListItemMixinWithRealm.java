package net.bytle.tower.eraldy.mixin;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


public abstract class ListItemMixinWithRealm {

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();


}
