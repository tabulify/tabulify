package net.bytle.tower.eraldy.api.implementer.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.vertx.collections.WriteThroughElementSerializer;
import org.jetbrains.annotations.NotNull;

public class ListImportJobSerializer implements WriteThroughElementSerializer<ListImportJob> {
  private final JsonMapper mapper;
  private final ListImportFlow listImportFlow;

  public ListImportJobSerializer(ListImportFlow listImportFlow) {
    this.listImportFlow = listImportFlow;
    this.mapper = listImportFlow.getApp().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder().build();
  }

  @Override
  public String getStoreId(@NotNull ListImportJob element) {
    return element.getStoreId();
  }

  @Override
  public String serialize(@NotNull ListImportJob element) throws CastException {
    try {
      return mapper.writeValueAsString(element.getStatus());
    } catch (JsonProcessingException e) {
      throw new CastException("Error while casting as json the list import job (" + element.getStoreId(), e);
    }
  }

  @Override
  public ListImportJob deserialize(@NotNull String value) throws CastException {
    ListImportJobStatus listImportStatus;
    try {
      listImportStatus = mapper.readValue(value, ListImportJobStatus.class);
    } catch (JsonProcessingException e) {
      throw new CastException("Error while building the list import status back with Jackson (value: " + value + ")", e);
    }
    return this.listImportFlow.createJobFromDatabase(listImportStatus);
  }

  @Override
  public void setStoreId(ListImportJob element, String id) {
    element.setStoreId(id);
  }
}
