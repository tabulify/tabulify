package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.realm.model.Realm;


public abstract class UserPublicMixinWithoutRealm {


  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();

  @JsonIgnore
  @JsonProperty("realm")
  abstract Realm getRealm();

}
