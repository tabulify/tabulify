package com.tabulify.flow.operation;

import com.tabulify.flow.engine.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.StrictException;
import com.tabulify.stream.InsertStream;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.type.*;
import net.bytle.type.yaml.YamlCast;

import java.util.*;
import java.util.stream.Collectors;

public class DefinePipelineStep extends PipelineStepRootBatchSupplierAbs {


  private final DefinePipelineStepBuilder definePipelineOperationProvider;

  public DefinePipelineStep(DefinePipelineStepBuilder definePipelineOperationProvider) {
    super(definePipelineOperationProvider);
    this.definePipelineOperationProvider = definePipelineOperationProvider;
  }

  @Override
  public Integer getNodeId() {
    return super.getNodeId();
  }

  public DataPath get() {

    return definePipelineOperationProvider.dataPaths.poll();

  }


  public static DefinePipelineStepBuilder builder() {
    return new DefinePipelineStepBuilder();
  }


  public boolean hasNext() {
    return !definePipelineOperationProvider.dataPaths.isEmpty();
  }

  @Override
  public PipelineStepIntermediateSupplier getIntermediateSupplier() {
    return definePipelineOperationProvider.stepConsumerSupplier;
  }


  public static class DefinePipelineStepBuilder extends PipelineStepBuilderStreamSupplier {

    public static final KeyNormalizer DEFINE = KeyNormalizer.createSafe("define");


    final ArrayDeque<DataPath> dataPaths = new ArrayDeque<>();

    /**
     * The step that may have created this instance
     */
    private PipelineStepIntermediateSupplier stepConsumerSupplier = null;
    private PipelineStepProcessingType processingType = PipelineStepProcessingType.BATCH;

    @Override
    public Boolean acceptOperation(KeyNormalizer operationName) {
      return DEFINE.equals(operationName);
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      ArrayList<Class<? extends ArgumentEnum>> list = new ArrayList<>(super.getArgumentEnums());
      list.add(DefinePipelineStepArgument.class);
      return list;
    }

    @Override
    public DefinePipelineStepBuilder createStepBuilder() {
      return new DefinePipelineStepBuilder();
    }

    @Override
    public PipelineStep build() {
      if (this.processingType == PipelineStepProcessingType.STREAM) {
        return new DefinePipelineStepStream(this);
      }
      return new DefinePipelineStep(this);
    }


    @Override
    public DefinePipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

      DefinePipelineStepArgument defineAttribute;
      try {
        defineAttribute = Casts.cast(key, DefinePipelineStepArgument.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The argument (" + key + ") is not an argument of the step (" + this + "). We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DefinePipelineStepArgument.class));
      }
      switch (defineAttribute) {
        case DATA_RESOURCES:
          /**
           * The processing variables
           */
          List<Object> listObjectDataResources;

          try {
            listObjectDataResources = Casts.castToNewList(value, Object.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The argument (" + defineAttribute + ") of the step (" + this + ") is not valid (not a list?). Error: " + e.getClass());
          }

          /**
           * Process
           */
          for (Object listObjectDataResource : listObjectDataResources) {
            buildProcessArgumentDataResource(YamlCast.castToMapKeyIndependent(listObjectDataResource, Object.class));
          }
          break;
        case DATA_RESOURCE:
          buildProcessArgumentDataResource(YamlCast.castToMapKeyIndependent(value, Object.class));
          break;
        case PROCESSING_TYPE:
          try {
            setProcessingType(Casts.cast(value, PipelineStepProcessingType.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The argument (" + defineAttribute + ") of the step (" + this + ") is not valid value. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(PipelineStepProcessingType.class));
          }
          break;
        default:
          throw new InternalException("The argument " + defineAttribute + " of the step (" + this + ") was not processed");
      }
      return this;

    }


    private void buildProcessArgumentDataResource(MapKeyIndependent<Object> dataResource) {

      Map<KeyNormalizer, Object> dataDef = new HashMap<>();
      List<List<Object>> records = new ArrayList<>();
      DataUriNode dataUri = null;
      MediaType mediaType = null;
      for (Map.Entry<String, Object> entryDataResource : dataResource.entrySet()) {
        String key = entryDataResource.getKey();
        DefinePipelineStepResourceArgument definePipelineOperationAttribute;
        try {
          definePipelineOperationAttribute = Casts.cast(key, DefinePipelineStepResourceArgument.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + key + ") is not a valid attributes for the step " + this + ". We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DefinePipelineStepResourceArgument.class), e);
        }
        Object value = entryDataResource.getValue();

        switch (definePipelineOperationAttribute) {
          case DATA_DEF:
            try {
              dataDef = Casts.castToNewMap(value, KeyNormalizer.class, Object.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The argument " + definePipelineOperationAttribute + " of the step " + this + " is not conform. (not a map?). Error: " + e.getMessage(), e);
            }
            break;
          case DATA_RECORDS:
            if (value == null) {
              if (this.getTabular().isStrictExecution()) {
                throw new StrictException("The argument " + definePipelineOperationAttribute + " of the step " + this + " is null");
              }
              continue;
            }
            try {
              records = Casts.castToNewList(value, Object.class)
                .stream()
                .map(e -> Casts.castToNewListSafe(e, Object.class))
                .collect(Collectors.toList());
            } catch (CastException e) {
              throw new IllegalArgumentException("The argument " + DefinePipelineStepResourceArgument.DATA_RECORDS + " of the step (" + this + ") is not a list but a " + value.getClass().getSimpleName() + ". Error: " + e.getMessage(), e);
            }
            break;
          case DATA_URI:
            dataUri = this.getTabular().createDataUri(value.toString());
            break;
          case MEDIA_TYPE:
            try {
              mediaType = MediaTypes.parse(value.toString());
            } catch (NullValueException e) {
              throw new IllegalArgumentException("The argument " + DefinePipelineStepResourceArgument.MEDIA_TYPE + " of the step (" + this + ")  has an invalid value " + ". Error: " + e.getMessage(), e);
            }
            break;
          default:
            throw new InternalException("The attribute `" + key + "` of the step (" + this + ") was not processed");
        }
      }
      if (dataUri != null && !records.isEmpty()) {
        throw new IllegalArgumentException("The argument " + DefinePipelineStepResourceArgument.DATA_URI + " and " + DefinePipelineStepResourceArgument.DATA_RECORDS + " cannot be set together");
      }

      DataPath dataPath;

      // data uri definition
      if (dataUri != null) {
        dataPath = getTabular().getDataPath(dataUri, mediaType);
        this.dataPaths.add(dataPath);
        if (dataDef != null) {
          dataPath.mergeDataDefinitionFromYamlMap(dataDef);
        }
        return;
      }

      // Generator Case
      if (mediaType != null) {
        dataPath = this.getTabular().getDataPath("", mediaType);
        this.dataPaths.add(dataPath);
        if (dataDef != null) {
          dataPath.mergeDataDefinitionFromYamlMap(dataDef);
        }
        return;
      }

      // inline records
      dataPath = this.getTabular().getAndCreateRandomMemoryDataPath();
      this.dataPaths.add(dataPath);
      if (records.isEmpty() && this.getPipeline().isStrict()) {
        throw new StrictException("The argument " + DefinePipelineStepResourceArgument.DATA_RECORDS + " of the step " + this + " was not given or is empty");
      }
      try (InsertStream insertStream = dataPath.getInsertStream()) {
        for (List<Object> data : records) {
          insertStream.insert(data);
        }
      }
      if (dataDef == null) {
        throw new IllegalArgumentException("The attribute " + DefinePipelineStepResourceArgument.DATA_DEF + " is mandatory when data is defined and was not found in the step (" + this + ")");
      }
      dataPath.mergeDataDefinitionFromYamlMap(dataDef);


    }


    public DefinePipelineStepBuilder addDataPath(DataPath dataPath) {
      this.dataPaths.add(dataPath);
      return this;
    }

    @Override
    public KeyNormalizer getOperationName() {
      return DEFINE;
    }

    public DefinePipelineStepBuilder addDataPaths(Collection<? extends DataPath> results) {
      this.dataPaths.addAll(results);
      return this;
    }

    public DefinePipelineStepBuilder setIntermediateSupplier(PipelineStepIntermediateSupplier stepConsumerSupplier) {
      this.stepConsumerSupplier = stepConsumerSupplier;
      return this;
    }

    public PipelineStepBuilder setProcessingType(PipelineStepProcessingType pipelineStepProcessingType) {
      this.processingType = pipelineStepProcessingType;
      return this;
    }
  }
}
