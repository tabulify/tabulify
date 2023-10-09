package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Key;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.yaml.YamlCast;

import java.util.*;

import static net.bytle.db.flow.step.TargetArguments.TARGET_DATA_DEFINITION;
import static net.bytle.db.flow.step.TargetArguments.TARGET_DATA_URI;

/**
 * A wrapper for all command that takes a target uri as attribute
 */
public abstract class TargetFilterStepAbs extends FilterStepAbs {

  protected DataUri targetUri;


  protected Map<String, ?> targetDataDef = new HashMap<>();

  public TargetFilterStepAbs() {

    this.getOrCreateArgument(TARGET_DATA_URI).setValueProvider(() -> this.targetUri);
    this.getOrCreateArgument(TARGET_DATA_DEFINITION).setValueProvider(() -> this.targetDataDef);

  }

  /**
   * @param targetUri The target uri
   * @return the step
   */
  public TargetFilterStepAbs setTargetUri(DataUri targetUri) {
    this.targetUri = targetUri;
    return this;
  }


  /**
   * Return a source map
   *
   * @param inputs the inputs paths
   * @return a map of source, target path
   */
  protected Map<DataPath, DataPath> getSourceTarget(Set<DataPath> inputs) {
    return SourceTargetHelperFunction
      .create(this.getTabular())
      .setTargetUri(this.targetUri)
      .setTargetDataDef(this.targetDataDef)
      .apply(inputs);
  }

  @Override
  public OperationStep setArguments(MapKeyIndependent<Object> arguments) {


    List<String> keyToRemoves = new ArrayList<>();
    for (Map.Entry<String, Object> targetEntry : arguments.entrySet()) {
      TargetArguments targetArguments;
      String key = targetEntry.getKey();
      try {
        targetArguments = Casts.cast(key, TargetArguments.class);
        keyToRemoves.add(key);
      } catch (CastException e) {
        // may be an argument of the next step
        continue;
      }
      switch (targetArguments) {
        case TARGET_DATA_DEFINITION:
          this.targetDataDef = YamlCast.castToSameMap(targetEntry.getValue(), String.class, Object.class);

          break;
        case TARGET_DATA_URI:
          targetUri = tabular.createDataUri(targetEntry.getValue().toString());
          break;
        default:
          throw new InternalException("The property `" + key + "` should have a branch for the step (" + this + ")");
      }
    }
    if (targetUri == null) {
      throw new IllegalStateException("The target data uri argument ("+ Key.toLongOptionName(TARGET_DATA_URI) +") is mandatory for the step (" + this + ") and was not found.");
    }

    /**
     * We remove the key founda
     * Otherwise the next will see it as an unknown argument
     * and may throw an error
     */
    for (String key : keyToRemoves) {
      arguments.remove(key);
    }

    return this;


  }


  public TargetFilterStepAbs setTargetDataDef(Map<String, ?> targetDataDef) {
    this.targetDataDef = targetDataDef;
    return this;
  }


}
