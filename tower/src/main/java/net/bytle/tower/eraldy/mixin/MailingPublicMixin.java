package net.bytle.tower.eraldy.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 *
 * You can not change mix-ins on-the-fly
 * <p>
 * Mix-ins are useful for applying Jackson configuration to classes
 * without modifying the source code of the target class.
 * Mix-ins can be thought of as a configuration layer that sits above a class
 * that Jackson will look at for instructions during de/serialization.
 * <a href="https://github.com/FasterXML/jackson-docs/wiki/JacksonMixInAnnotations">...</a>
 *
 */
public abstract class MailingPublicMixin {


  @JsonIgnore
  @JsonProperty("localId")
  abstract Long getLocalId();


}
