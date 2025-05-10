package com.tabulify.flow.engine;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.flow.FlowLog;
import com.tabulify.flow.stream.DataPathSupplier;
import com.tabulify.json.JsonObject;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.timer.Timer;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.Strings;
import net.bytle.type.yaml.YamlCast;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.tabulify.flow.engine.FlowStepAttribute.OPERATION;

/**
 * A flow/pipeline starts from one supplier
 * that is started after one event (trigger (manual, script), timer (cron, ...) or data event)
 * <p>
 * The supplier sends the data to one or more {@link PipelineStep#getDownStreamSteps()}
 * Ie Multiple response of one event can be coded in one play.
 * Example, you can subscribe to a file received in a directory
 * and apply different processing.
 */
public class Pipeline implements AutoCloseable {


  private final Tabular tabular;
  private DirectedAcyclicGraph<OperationStep, StepEdge> graph;
  /**
   * First step data supplier (ie data path)
   */
  private DataPathSupplier dataPathSupplier;

  /**
   * The runnable for accumulators step
   * are global reference in order to be able to send them
   * all inputs
   */
  private final Map<FilterOperationStep, FilterRunnable> filtersStep = new HashMap<>();

  /**
   * Data Path produced that were not consumed
   */
  private final Set<DataPath> notConsumedDataPaths = new HashSet<>();

  /**
   * The result of run (one row, one run)
   */
  private final Map<OperationStep, List<Set<DataPath>>> runsByStep = new HashMap<>();
  private OperationStep previous;
  private boolean hasBeenExecuted = false;

  public static Pipeline createFrom(Tabular tabular) {
    return new Pipeline(tabular);
  }

  public static Pipeline createFromYamlString(Tabular tabular, String yamlString) throws IllegalStructure {

    // The Yaml entry class
    Yaml yaml = new Yaml();

    Pipeline pipeline = create(tabular);


    // Load the yaml documents from the file
    List<Map<String, Object>> documents = new ArrayList<>();
    for (Object data : yaml.loadAll(yamlString)) {
      Map<String, Object> document;
      try {
        //noinspection unchecked
        document = (Map<String, Object>) data;
      } catch (ClassCastException e) {
        String message =
          Strings.createMultiLineFromStrings(
            "A flow file must be written as a yaml map format, not as a " + data.getClass().getSimpleName(),
            "The flow steps should be written in the `" + PipelineAttribute.PIPELINE + "` property."
          ).toString();
        throw new RuntimeException(message, e);
      }
      documents.add(document);
    }

    MapKeyIndependent<Object> play;

    switch (documents.size()) {
      case 0:
        throw new RuntimeException("We couldn't find any pipeline");
      case 1:
        play = new MapKeyIndependent<>();
        play.putAll(documents.get(0));
        break;
      default:
        throw new RuntimeException("We have found  (" + documents.size() + ") yaml documents but only one pipeline is supported by document.");
    }

    Object logicalName = play.get(PipelineAttribute.LOGICAL_NAME.toString());
    if (logicalName != null) {
      pipeline.setLogicalName(logicalName.toString());
    }

    Object pipelineObject = play.get(PipelineAttribute.PIPELINE.toString());
    if (pipelineObject == null) {
      throw new IllegalArgumentException("The `" + PipelineAttribute.PIPELINE + "` property could not be found");
    }
    List<Object> pipelineStepList;
    try {
      pipelineStepList = Casts.castToList(pipelineObject, Object.class);
    } catch (CastException e) {
      throw new RuntimeException("The pipeline value is not a map but a " + pipelineObject.getClass().getSimpleName(), e);
    }

    /*
     * The known operation
     */
    List<StepProvider> operationsRegistered = StepProvider.installedProviders();


    if (pipelineStepList.isEmpty()) {
      throw new RuntimeException("The yaml does not have any step. Nothing to do");
    }


    /*
     * Loop through the steps (ie list of maps)
     */
    int stepCounter = 0;

    for (Object stepObject : pipelineStepList) {


      // Init
      stepCounter++;
      String stepName = "step" + stepCounter;
      String stepDescription = null;
      String operationString = null;
      MapKeyIndependent<Object> arguments = new MapKeyIndependent<>();

      // Loop over the step attribute
      Map<String, Object> stepMap;
      try {
        stepMap = Casts.castToSameMap(stepObject, String.class, Object.class);
      } catch (CastException e) {
        throw new InternalException("String and Object should not throw a cast exception", e);
      }
      for (Map.Entry<String, Object> stepEntry : stepMap.entrySet()) {
        String key = stepEntry.getKey();
        Object value = stepEntry.getValue();
        if (value == null) {
          throw new IllegalStructure("The attribute (" + key + ") on the step (" + stepName + ") has a null value");
        }
        FlowStepAttribute stepAttribute;
        KeyNormalizer keyNormalized = KeyNormalizer.createSafe(key);
        if (keyNormalized.equals(KeyNormalizer.createSafe("args"))) {
          stepAttribute = FlowStepAttribute.ARGUMENTS;
        } else if (keyNormalized.equals(KeyNormalizer.createSafe("op"))) {
          stepAttribute = FlowStepAttribute.OPERATION;
        } else {
          try {
            stepAttribute = Casts.cast(key, FlowStepAttribute.class);
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createForStepArgument(key, stepName, FlowStepAttribute.class, e);
          }
        }
        switch (stepAttribute) {
          case NAME:
            stepName = value.toString();
            break;
          case OPERATION:
            operationString = value.toString();
            break;
          case COMMENT:
            stepDescription = value.toString();
            break;
          case ARGUMENTS:
            if (!(value instanceof Map)) {
              throw new IllegalStructure("The arguments of the step (" + stepName + ") are not a list of key-pair values (ie a map) but a " + value.getClass().getSimpleName());
            }
            try {
              arguments = YamlCast.castToSameMap(value, String.class, Object.class)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (e1, e2) -> e1,
                  MapKeyIndependent::new
                ));
            } catch (CastException e) {
              throw new InternalException("String and Object should not throw a cast exception", e);
            }
            break;
          default:
            throw new InternalError("Error while reading the pipeline file, the step attribute (" + stepAttribute + ") should have a branch");
        }

      }

      if (operationString == null) {
        throw new IllegalStructure("We were unable to find the (" + KeyNormalizer.createSafe(OPERATION).toCliLongOptionName() + ") argument in the " + stepName + " step (Data: " + stepMap + ")");
      }


      OperationStep step = null;
      for (StepProvider operationProvided : operationsRegistered) {
        if (operationProvided.accept(operationString)) {
          step = operationProvided.createStep();
        }
      }
      if (step == null) {
        List<String> registeredNames = operationsRegistered
          .stream()
          .flatMap(opRegistered -> opRegistered.getAcceptedCommandNames().stream())
          .sorted()
          .distinct()
          .collect(Collectors.toList());
        throw new IllegalStructure("The operation (" + operationString + ") is unknown or unregistered for the step (" + stepName + ") step. The operations available are (" + String.join(", ", registeredNames) + ").");
      }


      try {
        step.setName(stepName);
      } catch (RuntimeException e) {
        throw new RuntimeException("The step name (`" + stepName + "`) is not compliant: " + e.getMessage());
      }
      if (stepDescription != null) {
        step.setDescription(stepDescription);
      }
      /**
       * Inject Tabular
       */
      step.setTabular(tabular);
      FlowLog.LOGGER.fine("Step `" + stepName + "` with the operation " + step.getOperationName() + " found");

      if (!arguments.isEmpty()) {
        step.setArguments(arguments);
      }

      pipeline.addStepToGraph(step);

    }
    return pipeline;
  }

  public static Pipeline createFromYamlPath(Tabular tabular, Path path) {


    FlowLog.LOGGER.fine("Parsing the flow file (" + path + ")");
    String yamlString;
    try {
      yamlString = Fs.getFileContent(path);
    } catch (NoSuchFileException e) {
      throw new IllegalStateException("The pipeline file (" + path + ") does not exists");
    }


    tabular.setParsedPipelineScript(path);
    try {
      return createFromYamlString(tabular, yamlString)
        .setPath(path);
    } catch (Exception e) {
      throw new RuntimeException("Error while parsing the pipeline file (" + path + "). Error: " + e.getMessage(), e);
    } finally {
      tabular.setParsedPipelineScript(null);
    }

  }


  public String getLogicalName() {
    if (logicalName == null) {
      if (path != null) {
        return Fs.getFileNameWithoutExtension(path);
      } else {
        return "Anonymous";
      }
    } else {
      return logicalName;
    }
  }

  private String logicalName;
  private Timer timer;
  private Path path;


  public Pipeline(Tabular tabular) {
    this.tabular = tabular;
  }

  public static Pipeline create(Tabular tabular) {
    return new Pipeline(tabular);
  }

  public Pipeline execute() {

    if (graph == null) {
      throw new IllegalStateException("The graph of execution is null. We can't execute it.");
    }
    this.hasBeenExecuted = true;
    try {
      this.timer = Timer.createFromUuid().start();
      /**
       * The runner type
       */
      FlowLog.LOGGER.info("Executing the step " + dataPathSupplier);
      DescendantRunner descendantRunner = createDescendantRunner(dataPathSupplier);
      int run = 0;
      while (dataPathSupplier.hasNext()) {
        run++;
        Set<DataPath> next = dataPathSupplier.next();
        Objects.requireNonNull(next);
        this.addRunResult(dataPathSupplier, next);
        descendantRunner.run(next);
      }
      if (run == 0) {
        this.addRunResult(dataPathSupplier, new HashSet<>());
      }
      FlowLog.LOGGER.info("Step " + dataPathSupplier + " executed");

      /**
       * If we have filter step, run them
       */
      while (!filtersStep.isEmpty()) {
        for (Map.Entry<FilterOperationStep, FilterRunnable> entry : filtersStep.entrySet()) {
          FilterOperationStep filterOperationStep = entry.getKey();
          filtersStep.remove(filterOperationStep);
          FlowLog.LOGGER.info("Executing the step " + filterOperationStep);
          this.runTraverse(filterOperationStep, entry.getValue());
          FlowLog.LOGGER.info("Step " + filterOperationStep + " executed");
        }
      }

      this.timer.stop();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  private void addRunResult(OperationStep operationStep, Set<DataPath> dataPaths) {
    Objects.requireNonNull(dataPaths);
    List<Set<DataPath>> list = this.runsByStep.getOrDefault(operationStep, new ArrayList<>());
    list.add(dataPaths);
    this.runsByStep.put(operationStep, list);
  }

  private DescendantRunner createDescendantRunner(OperationStep operationStep) {
    return new DescendantRunner(this, operationStep);
  }

  public Set<DataPath> getDownStreamDataPaths() {
    if (!hasBeenExecuted) {
      execute().close();
    }
    return this.notConsumedDataPaths;
  }

  public DataPath getRunsByStepDataPath() {

    InsertStream insertStream = this.tabular.getMemoryDataStore().getAndCreateRandomDataPath()
      .setLogicalName("flow_executions")
      .setDescription("List of executions")
      .createRelationDef()
      .addColumn("ExecutionId")
      .addColumn("OperationName")
      .addColumn("DataPathsCount")
      .addColumn("DataPathsList")
      .getDataPath()
      .getInsertStream();

    if (!runsByStep.isEmpty()) {
      TopologicalOrderIterator<OperationStep, StepEdge> orderIterator = new TopologicalOrderIterator<>(graph);
      int runCounter = 0;
      while (orderIterator.hasNext()) {
        OperationStep operationStep = orderIterator.next();
        List<Set<DataPath>> runs = this.runsByStep.get(operationStep);
        if (runs != null) {
          for (Set<DataPath> run : runs) {
            runCounter++;
            String dps = run.stream().sorted().map(dp -> {
                if (dp instanceof MemoryDataPath) {
                  return dp.getLogicalName() + "@" + dp.getConnection().getName();
                } else {
                  return dp.toDataUri().toString();
                }
              }
            ).collect(Collectors.joining(", "));
            insertStream.insert(runCounter, operationStep.getOperationName(), run.size(), dps);
          }
        }
      }
    }
    insertStream.close();
    return insertStream.getDataPath();
  }

  public JsonObject getJsonResult() {


    JsonObject result = JsonObject.create();

    result.addProperty("logicalName", this.getLogicalName())
      .addProperty("filePath", this.path.toAbsolutePath())
      .addProperty("startTime", timer.getStartTime())
      .addProperty("endTime", timer.getEndTime());


    TopologicalOrderIterator<OperationStep, StepEdge> orderIterator = new TopologicalOrderIterator<>(graph);
    List<JsonObject> jsonObjects = new ArrayList<>();
    while (orderIterator.hasNext()) {
      OperationStep operationStep = orderIterator.next();
      JsonObject operationJson = JsonObject.create();
      jsonObjects.add(operationJson);
      operationJson
        .addProperty("name", operationStep.getName())
        .addProperty("operation", operationStep.getOperationName());
      JsonObject args = operationJson.createChildObject("args");
      for (Attribute attribute : operationStep.getArguments()) {
        Object value = attribute.getValueOrDefaultOrNull();
        String publicName = tabular.toPublicName(attribute.getAttributeMetadata().toString());
        if (value != null) {
          args.addProperty(publicName, value);
        } else {
          args.addProperty(publicName, "");
        }
      }
    }
    result.addProperty("operations", jsonObjects);


    return result;


  }

  public Pipeline setLogicalName(String logicalName) {
    this.logicalName = logicalName;
    return this;
  }

  public Pipeline setPath(Path path) {
    this.path = path;
    return this;
  }

  public Pipeline addStepToGraph(OperationStep step) {

    if (graph == null) {
      if (!DataPathSupplier.class.isAssignableFrom(step.getClass())) {
        throw new RuntimeException("The first step (" + step.getName() + ") of the flow (" + this + ") is not a data resource supplier operation.");
      }
      graph = new DirectedAcyclicGraph<>(StepEdge.class);
      this.dataPathSupplier = (DataPathSupplier) step;
    }


    step.setTabular(this.tabular);

    /**
     * Name
     * This is the unique identifier in the flow
     * It must be computed before adding it to the graph
     */
    if (step.getName() == null) {
      step.setName("step" + (graph.vertexSet().size() + 1));
    }

    if (graph.containsVertex(step)) {
      throw new IllegalStateException("The step name (" + step.getName() + ") is already defined in the flow. Choose another one");
    }

    graph.addVertex(step);
    if (this.previous != null) {
      graph.addEdge(previous, step);
    }

    previous = step;
    return this;
  }

  @Override
  public void close() {
//    if (FD != null) {
//      tabular.dropConnection(FD);
//    }
  }

  public int getRunCount() {
    if (!hasBeenExecuted) {
      execute().close();
    }
    return this.runsByStep.values().stream()
      .mapToInt(List::size)
      .sum();
  }

  public List<Set<DataPath>> getResult(String sup) {
    return this.runsByStep
      .entrySet()
      .stream()
      .filter(e -> e.getKey().getName().equals(sup))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  public int size() {
    return graph.vertexSet().size();
  }

  /**
   * A descendant runner that permits
   * to keep a reference to the runner of the node
   * <p>
   * This is used when a function is an accumulator and needs to get the whole stream before running
   * <p>
   * When the descendantRunner close, it will execute the accumulator step
   */
  private class DescendantRunner {

    private final List<FilterOperationStep> descendants;
    private final Pipeline pipeline;
    private final OperationStep operationStep;


    public DescendantRunner(Pipeline pipeline, OperationStep parentOperationStep) {
      this.pipeline = pipeline;
      descendants = Casts.castToListSafe(Graphs.successorListOf(graph, parentOperationStep), FilterOperationStep.class);
      this.operationStep = parentOperationStep;
    }

    @Override
    public String toString() {
      return "Runner for " + this.operationStep;
    }

    public void run(Set<DataPath> parentDataPaths) throws ExecutionException, InterruptedException {

      if (!descendants.isEmpty()) {
        for (FilterOperationStep descendantStep : descendants) {

          FilterRunnable filterRunnable;
          if (descendantStep.isAccumulator()) {
            filterRunnable = filtersStep.computeIfAbsent(descendantStep, FilterOperationStep::createRunnable);
            filterRunnable.addInput(parentDataPaths);
          } else {
            filterRunnable = descendantStep.createRunnable();
            filterRunnable.addInput(parentDataPaths);
            runTraverse(descendantStep, filterRunnable);
          }
        }
      } else {

        this.pipeline.addNotConsumedDataPaths(parentDataPaths);

      }

    }


  }

  private void addNotConsumedDataPaths(Set<DataPath> notConsumedDataPaths) {
    this.notConsumedDataPaths.addAll(notConsumedDataPaths);
  }

  private void runTraverse(FilterOperationStep descendant, FilterRunnable filterRunnable) throws InterruptedException, ExecutionException {
    filterRunnable.run();
    Set<DataPath> next = filterRunnable.get();
    Objects.requireNonNull(next);
    this.addRunResult(descendant, next);
    DescendantRunner descendantRunner = createDescendantRunner(descendant);
    descendantRunner.run(next);
  }

}
