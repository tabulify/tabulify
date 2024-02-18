package net.bytle.tower.eraldy.api.implementer.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.vertx.collections.CollectionWriteThroughSerializer;
import org.jetbrains.annotations.NotNull;

public class ListImportJobSerializer implements CollectionWriteThroughSerializer<ListImportJob> {
  private final JsonMapper mapper;
  private final ListImportFlow listImportFlow;

  public ListImportJobSerializer(ListImportFlow listImportFlow) {
    this.listImportFlow = listImportFlow;
    this.mapper = listImportFlow.getApp().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder().build();
  }

  @Override
  public String getObjectId(@NotNull ListImportJob value) {
    return value.getGuid();
  }

  @Override
  public String serialize(@NotNull ListImportJob value) throws CastException {
    try {
      return mapper.writeValueAsString(value.getStatus());
    } catch (JsonProcessingException e) {
      throw new CastException("Error while casting as json the list import job ("+value.getGuid(),e);
    }
  }

  @Override
  public ListImportJob deserialize(@NotNull String input) {
    ListImportJobStatus listImportStatus = mapper.convertValue(input, ListImportJobStatus.class);
    return this.listImportFlow.createJobFromDatabase(listImportStatus);
  }
}
