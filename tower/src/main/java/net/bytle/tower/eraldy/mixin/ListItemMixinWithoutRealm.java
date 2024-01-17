package net.bytle.tower.eraldy.mixin;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.Realm;


public abstract class ListItemMixinWithoutRealm {

  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();

  @JsonIgnore
  @JsonProperty("realm")
  abstract Realm getRealm();

}
