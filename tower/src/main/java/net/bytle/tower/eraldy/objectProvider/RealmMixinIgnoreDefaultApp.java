package net.bytle.tower.eraldy.objectProvider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.App;


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
abstract class RealmMixinIgnoreDefaultApp {

  @JsonIgnore
  App defaultApp;

  @JsonIgnore
  @JsonProperty("defaultApp")
  abstract App getDefaultApp();

}
