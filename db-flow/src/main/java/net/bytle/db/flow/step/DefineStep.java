package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.StepAbs;
import net.bytle.db.flow.stream.DataPathSupplier;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.Key;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.yaml.YamlCast;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefineStep extends StepAbs implements DataPathSupplier {


  private List<Set<DataPath>> buildDataPaths;
  private final Set<DataPath> dataPaths = new HashSet<>();
  private Iterator<Set<DataPath>> iterator;
  private Map<String, ?> attributes = new HashMap<>();
  private final Set<DataUri> dataUris = new HashSet<>();

  public DefineStep() {

    this.getOrCreateArgument(DefineStepAttribute.DATA_URI).setValueProvider(()->this.dataUris);

  }

  public static DefineStep create() {
    return new DefineStep();
  }


  @Override
  public Set<DataPath> get() {


    buildIfNeeded();
    return iterator.next();

  }

  private void buildIfNeeded() {

    if (iterator != null) {
      return;
    }
    buildDataPaths = dataUris
      .stream()
      .map(du -> tabular.getDataPath(du.toString()))
      .sorted()
      .map(dp -> dp.setDataAttributes(attributes))
      .map(CastStep.create())
      .map(dp -> Stream.of(dp).collect(Collectors.toSet()))
      .collect(Collectors.toList());
    if (dataPaths.size() > 0) {
      buildDataPaths.add(dataPaths);
    }
    iterator = buildDataPaths.iterator();
  }

  @Override
  public int getSetCount() {
    return buildDataPaths.size();
  }

  @Override
  public boolean hasNext() {
    buildIfNeeded();
    return iterator.hasNext();
  }

  @Override
  public Set<DataPath> next() {
    buildIfNeeded();
    return iterator.next();
  }

  @Override
  public String getOperationName() {
    return "define";
  }


  public DefineStep setAttributes(Map<String, ?> attributes) {
    this.attributes = attributes;
    return this;
  }

  public DefineStep addUri(DataUri dataUri) {
    this.dataUris.add(dataUri);
    return this;
  }


  public DefineStep addDataPath(DataPath dataPath) {
    this.dataPaths.add(dataPath);
    return this;
  }

  @Override
  public DefineStep setArguments(MapKeyIndependent<Object> arguments) {
    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
      DefineStepAttribute defineAttribute;
      try {
        defineAttribute = Casts.cast(entry.getKey(), DefineStepAttribute.class);
      } catch (CastException e) {
        throw new IllegalStateException("The attribute (" + entry.getKey() + ") is not an attribute of the step (" + this + ")");
      }
      switch (defineAttribute) {
        case DATA_RESOURCE:
        case DATA_RESOURCES:
          /**
           * The processing variables
           */
          List<Object> listObjectDataResources;
          // if list or map
          if (entry.getValue() instanceof List) {
            listObjectDataResources = Casts.castToListSafe(entry.getValue(), Object.class);
          } else {
            listObjectDataResources = Collections.singletonList(YamlCast.castToMapKeyIndependent(entry.getValue(), Object.class));
          }
          /**
           * Process
           */
          for (Object listObjectDataResource : listObjectDataResources) {
            buildAndAddDataResource(YamlCast.castToMapKeyIndependent(listObjectDataResource, Object.class));
          }
          break;
        case DATA_DEFINITION:


      }
    }
    return this;
  }


  private void buildAndAddDataResource(MapKeyIndependent<Object> dataResource) {

    DataPath dataPath = null;
    List<List<Object>> records = null;
    for (Map.Entry<String, Object> entryDataResource : dataResource.entrySet()) {
      switch (Key.toNormalizedKey(entryDataResource.getKey())) {
        case "datadefinition":
        case "datadef":
          Map<String, Object> dataDef = YamlCast.castToSameMap(entryDataResource.getValue(), String.class, Object.class);
          dataPath = tabular.getAndCreateRandomMemoryDataPath()
            .mergeDataDefinitionFromYamlMap(dataDef);
          dataPaths.add(dataPath);
          break;
        case "data":
        case "datas":
          records = Casts.castToListSafe(entryDataResource.getValue(), Object.class)
            .stream()
            .map(e -> Casts.castToListSafe(e, Object.class))
            .collect(Collectors.toList());
          break;
        default:
          throw new IllegalStateException("The property `" + entryDataResource.getKey() + "` of the 'data-resource' argument of the step (" + this + ") is unknown.");
      }
    }
    if (dataPath == null) {
      throw new RuntimeException("The property `data-ref` is mandatory in a `dataresource` argument and was not found in the step (" + this + ")");
    }
    if (records != null) {
      try (InsertStream insertStream = dataPath.getInsertStream()) {
        for (List<Object> data : records) {
          insertStream.insert(data);
        }
      }
    }
  }
}

