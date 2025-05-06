package com.tabulify.flow.step;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.enrich.EnrichDataPath;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tabulify.flow.step.EnrichStep.EnrichStepArgument.VIRTUAL_COLUMN;

public class EnrichStep extends FilterStepAbs implements Function<Set<DataPath>, Set<DataPath>> {

  public EnrichStep() {

    this.getOrCreateArgument(EnrichStepArgument.VIRTUAL_COLUMNS).setValueProvider(this::getVirtualColumns);
  }

  private String getVirtualColumns() {
    return this.virtualColumns
      .stream()
      .map(e -> e.getColumnName() + e.getDataPathAttribute())
      .collect(Collectors.joining(", "));
  }

  public EnrichStep addVirtualColumns(Map<String, String> virtualColumns) {
    for (Map.Entry<String, String> stringStringEntry : virtualColumns.entrySet()) {
      String value = stringStringEntry.getValue();
      DataPathAttribute dataPathAttribute;
      try {
        dataPathAttribute = Casts.cast(value, DataPathAttribute.class);
      } catch (CastException e) {
        throw new IllegalStateException("The variable value (" + value + ") of the step (" + this + ") is not a path variable and can't be added as virtual column.");
      }
      addVirtualColumn(stringStringEntry.getKey(), dataPathAttribute);
    }
    return this;
  }

  public enum EnrichStepArgument implements AttributeEnum {

    VIRTUAL_COLUMN("The virtual column definition"),
    VIRTUAL_COLUMNS("The virtual columns definitions");


    private final String description;


    EnrichStepArgument(String description) {

      this.description = description;

    }


    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public Class<?> getValueClazz() {
      return null;
    }

    @Override
    public Object getDefaultValue() {
      return null;
    }

  }


  List<VirtualColumn> virtualColumns = new ArrayList<>();

  public static EnrichStep create() {
    return new EnrichStep();
  }

  @Override
  public Set<DataPath> apply(Set<DataPath> dataPaths) {

    if (this.virtualColumns.isEmpty()) {
      return dataPaths;
    }
    Set<DataPath> outputs = new HashSet<>();
    for (DataPath input : dataPaths) {
      EnrichDataPath enrichDataPath = EnrichDataPath.create(input);
      outputs.add(enrichDataPath);
      for (VirtualColumn virtualColumn : virtualColumns) {
        enrichDataPath.addVirtualColumn(virtualColumn.getColumnName(), virtualColumn.getDataPathAttribute());
      }
    }
    return outputs;


  }

  public EnrichStep addVirtualColumnFromAttribute(DataPathAttribute dataPathAttribute) {
    this.virtualColumns.add(new VirtualColumn(KeyNormalizer.createSafe(dataPathAttribute).toSqlCaseSafe(), dataPathAttribute));
    return this;
  }

  /**
   * @param columnName        - column name given by the user
   * @param dataPathAttribute - the data path attribute
   * @return the object for chaining
   */
  public EnrichStep addVirtualColumn(String columnName, DataPathAttribute dataPathAttribute) {
    this.virtualColumns.add(new VirtualColumn(columnName, dataPathAttribute));
    return this;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new EnrichRunnable(this);
  }

  @Override
  public String getOperationName() {
    return "enrich";
  }


  @Override
  public EnrichStep setArguments(MapKeyIndependent<Object> arguments) {
    for (Map.Entry<String, Object> entry : arguments.entrySet()) {

      try {
        Casts.cast(entry.getKey(), EnrichStepArgument.class);
      } catch (CastException e) {
        throw new IllegalStateException("The argument (" + entry.getKey() + ") is not valid argument for the step (" + this + ")");
      }

      Object value = entry.getValue();
      if (value instanceof String) {
        DataPathAttribute dpAtt;
        try {
          dpAtt = Casts.cast(value.toString(), DataPathAttribute.class);
        } catch (CastException e) {
          throw new IllegalStateException("The variable value (" + value + ") of the step (" + this + ") is not a path variable and can't be added as virtual column.");
        }
        this.addVirtualColumnFromAttribute(dpAtt);
      } else if (value instanceof Map) {
        Map<String, String> map;
        try {
          map = Casts.castToSameMap(value, String.class, String.class);
        } catch (CastException e) {
          throw new InternalException("String and String should not throw a cast exception", e);
        }
        for (Map.Entry<String, String> virtualColumnEntry : map.entrySet()) {
          DataPathAttribute dpAtt;
          try {
            dpAtt = Casts.cast(virtualColumnEntry.getValue(), DataPathAttribute.class);
          } catch (CastException e) {
            throw new IllegalStateException("The variable value (" + value + ") of the step (" + this + ") is not a path variable and can't be added as virtual column.");
          }
          this.addVirtualColumn(virtualColumnEntry.getKey(), dpAtt);
        }
      } else if (value instanceof List) {
        List<Object> virtualColumns = Casts.castToListSafe(value, Object.class);
        for (Object virtualColumn : virtualColumns) {
          if (virtualColumn instanceof String) {
            DataPathAttribute dpAtt;
            try {
              dpAtt = Casts.cast(value.toString(), DataPathAttribute.class);
            } catch (CastException e) {
              throw new IllegalStateException("The variable value (" + value + ") of the step (" + this + ") is not a path variable and can't be added as virtual column.");
            }
            this.addVirtualColumnFromAttribute(dpAtt);
          } else if (virtualColumn instanceof Map) {
            Map<String, String> virtualColumnMap;
            try {
              virtualColumnMap = Casts.castToSameMap(virtualColumn, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String and String should not throw a cast exception", e);
            }
            String resourceAttributePublicKey = "resource-attribute";

            String name = null;
            String resourceAttribute = null;

            for (Map.Entry<String, String> virtualColumnEntry : virtualColumnMap.entrySet()) {
              String normalizedVirtualkeyEntry = KeyNormalizer.createSafe(virtualColumnEntry.getKey()).toHyphenCase();
              switch (normalizedVirtualkeyEntry) {
                case "name":
                  name = virtualColumnEntry.getValue();
                  break;
                case "resource-attribute":
                  resourceAttribute = virtualColumnEntry.getValue();
                  break;
                default:
                  throw new IllegalStateException("The property (" + virtualColumnEntry.getKey() + ") is unknown for the definition of a virtual column in the step (" + this + ")");
              }

            }
            if (resourceAttribute == null) {

              throw new IllegalStateException("The property (" + resourceAttributePublicKey + ") is mandatory when creating a virtual column in the step (" + this + ") and was not found.");
            }
            if (name == null) {
              name = resourceAttribute;
            }
            DataPathAttribute resourceAttributeObject;
            try {
              resourceAttributeObject = Casts.cast(resourceAttribute, DataPathAttribute.class);
            } catch (CastException e) {
              throw new IllegalStateException("The variable value (" + resourceAttribute + ") of the step (" + this + ") is not a path variable and can't be added as virtual column.");
            }
            this.addVirtualColumn(name, resourceAttributeObject);
          }
        }
      } else {
        throw new IllegalStateException("The virtual column entry (" + VIRTUAL_COLUMN + ") should be a string or a list of map but is a " + value.getClass().getSimpleName() + ". The data of the property are " + value);
      }

    }
    return this;
  }

  /**
   * A utility object
   */
  private static class VirtualColumn {
    private final DataPathAttribute dataPathAttribute;
    private final String columnName;

    public VirtualColumn(String columnName, DataPathAttribute dataPathAttribute) {
      this.columnName = columnName;
      this.dataPathAttribute = dataPathAttribute;
    }

    public DataPathAttribute getDataPathAttribute() {
      return this.dataPathAttribute;
    }

    public String getColumnName() {
      return this.columnName;
    }
  }

  private static class EnrichRunnable implements FilterRunnable {

    private final EnrichStep enrichStep;
    private final Set<DataPath> allInputs = new HashSet<>();
    private boolean isDone = false;
    private Set<DataPath> allOutput = new HashSet<>();

    public EnrichRunnable(EnrichStep enrichStep) {
      this.enrichStep = enrichStep;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.allInputs.addAll(inputs);
    }

    @Override
    public void run() {
      allOutput = this.enrichStep.apply(allInputs);
      isDone = true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return isDone;
    }

    @Override
    public Set<DataPath> get() {
      return this.allOutput;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) {
      return get();
    }
  }
}
