package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonRealmGuidSerializer extends JacksonJsonStringSerializer<RealmGuid> {



  private final GuidDeSer guidDeSer;
  public JacksonRealmGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(RealmGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(RealmGuid realmGuid) {
    return guidDeSer.serialize(realmGuid.getLocalId());
  }

}
