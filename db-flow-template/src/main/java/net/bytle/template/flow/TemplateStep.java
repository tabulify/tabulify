package net.bytle.template.flow;

import net.bytle.crypto.Digest;
import net.bytle.db.flow.Granularity;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.spi.SelectException;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.*;
import net.bytle.html.CssInliner;
import net.bytle.template.JsonTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.template.ThymeleafTemplateEngine;
import net.bytle.template.api.Template;
import net.bytle.type.*;
import net.bytle.type.yaml.YamlCast;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.bytle.template.flow.TemplateAttributes.STEP_GRANULARITY;
import static net.bytle.template.flow.TemplateAttributes.STEP_OUTPUT;
import static net.bytle.template.flow.TemplateEngine.NATIVE;
import static net.bytle.template.flow.TemplateEngine.THYMELEAF;
import static net.bytle.template.flow.TemplateOutputOperation.EXTENDED_RECORDS;
import static net.bytle.template.flow.TemplateOutputOperation.TEMPLATES;
import static net.bytle.type.MediaTypeExtension.JSON_EXTENSION;
import static net.bytle.type.MediaTypeExtension.TEXT_EXTENSION;

public class TemplateStep extends FilterStepAbs implements OperationStep {


  /**
   * The environment data path variable
   * from the template are added to the engine
   * with this prefix
   * (The source variable does not have any prefix)
   */
  public static final String TEMPLATE_PREFIX = "template-";
  private final Set<DataUri> templateSelectors = new HashSet<>();
  private final Map<String, TemplateModel> inlineTemplates = new HashMap<>();
  private Granularity granularity;
  private net.bytle.template.flow.TemplateEngine templateEngine;
  private final Map<String, DataUri> tableVariables = new HashMap<>();
  private TemplateOutputOperation templateOutputOperation = TEMPLATES;
  private String outputLogicalName;
  private Boolean isTemplateEmail = false;

  /**
   * The data path attributes added to the environment
   * We make a selection to be sure that we are not calculating
   * derived attribute such as count, md5, ...
   */
  private final List<DataPathAttribute> dataPathAttributesAddedAsEnv = Arrays.asList(
    DataPathAttribute.NAME,
    DataPathAttribute.LOGICAL_NAME,
    DataPathAttribute.TYPE,
    DataPathAttribute.SUBTYPE
  );


  public static TemplateStep create() {
    return new TemplateStep();
  }

  public TemplateStep addTemplateSelectors(Set<DataUri> dataSelectors) {
    this.templateSelectors.addAll(dataSelectors);
    return this;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new TemplateRunnable(this);
  }

  @Override
  public String getOperationName() {
    return "template";
  }


  public TemplateStep addTemplateSelector(DataUri dataSelector) {
    this.templateSelectors.add(dataSelector);
    return this;
  }

  public TemplateStep setTemplateEngine(net.bytle.template.flow.TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
    return this;
  }

  public TemplateStep addTemplateTabularVariable(String name, DataUri tableSelector) {
    this.tableVariables.put(name, tableSelector);
    return this;
  }

  public TemplateStep setOutputLogicalName(String outputLogicalName) {
    this.outputLogicalName = outputLogicalName;
    return this;
  }


  private class TemplateRunnable implements FilterRunnable {

    private final TemplateStep templateStep;
    private final Set<DataPath> inputs = new HashSet<>();
    private final Set<DataPath> outputs = new HashSet<>();
    private boolean isDone = false;

    public TemplateRunnable(TemplateStep templateStep) {
      this.templateStep = templateStep;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {

      Set<TemplateModel> templateModels = new HashSet<>(TemplateStep.this.inlineTemplates.values());

      /**
       * Template Selectors if any
       */
      Set<DataPath> templateDataPaths;
      if (!templateSelectors.isEmpty()) {
        templateDataPaths = tabular.select(templateSelectors, false, null);
        if (templateDataPaths.isEmpty()) {
          throw new RuntimeException("No templates were selected by the selectors (" + templateSelectors.stream().map(DataUri::toString).collect(Collectors.joining(",")));
        }
        for (DataPath templateDataPath : templateDataPaths) {

          templateModels.add(TemplateModel.createFromDataPath(templateDataPath));
        }
      }

      /**
       * Flow Granularity and engine definition
       */
      boolean hierarchicalTemplateFound = false;
      for (TemplateModel templateModel : templateModels) {


        MediaType mediaTypeType = templateModel.getMediaType();

        /**
         * Default template engine and execution
         */
        switch (mediaTypeType.getExtension()) {
          case JSON_EXTENSION:
            // hierarchical structure
            if (templateEngine == null) {
              templateEngine = NATIVE;
            }
            if (granularity == null) {
              granularity = Granularity.RESOURCE;
            }
            hierarchicalTemplateFound = true;
            break;
          case TEXT_EXTENSION:
            if (hierarchicalTemplateFound) {
              throw new RuntimeException("You cannot mixed a hierarchical template (json) with a non-hierarchical template (txt) in the same step");
            }
            if (templateEngine == null) {
              templateEngine = NATIVE;
            }
            if (granularity == null) {
              granularity = Granularity.RECORD;
            }
            break;
          default:
            if (hierarchicalTemplateFound) {
              throw new RuntimeException("You cannot mixed a hierarchical template (json) with a non-hierarchical template (" + templateModel.getMediaType().getSubType() + ") in the same step");
            }
            if (templateEngine == null) {
              templateEngine = THYMELEAF;
            }
            if (granularity == null) {
              granularity = Granularity.RECORD;
            }
        }
      }

      /**
       * The env variables used to generate the
       * output logical name
       */
      Map<String, Object> envVariables = new HashMap<>();


      /**
       * For each data path
       */
      for (DataPath source : this.inputs) {

        /**
         * Env variable for the source
         */
        for (DataPathAttribute envVariablesDataPathAttribute : dataPathAttributesAddedAsEnv) {
          try {
            envVariables.put(Key.toLongOptionName(envVariablesDataPathAttribute), source.getVariable(envVariablesDataPathAttribute));
          } catch (NoVariableException ex) {
            // ok
          }
        }


        /**
         *
         */
        if (this.templateStep.granularity == Granularity.RESOURCE) {
          try {
            this.runResourceLevel(
              templateModels,
              envVariables,
              source
            );
          } catch (SelectException e) {
            throw new RuntimeException("Error while running the template step on a resource level (ie hierarchical templating) (" + this + ". Error:" + e.getMessage());
          }
        } else {
          this.runRecordLevel(
            templateModels,
            envVariables,
            dataPathAttributesAddedAsEnv,
            source
          );
        }
      }

      isDone = true;
    }

    private void runRecordLevel(
      Set<TemplateModel> templateModels,
      Map<String, Object> envVariables,
      List<DataPathAttribute> envVariablesDataPathAttributes,
      DataPath source
    ) {
      /**
       * If the output is the extended format, we create
       * a copy of the source and add a column by template
       */
      InsertStream enrichInsertStream = null;
      List<String> enrichedTemplateResults = new ArrayList<>();
      if (this.templateStep.templateOutputOperation == EXTENDED_RECORDS) {

        RelationDef enrichOutputDef = tabular.getAndCreateRandomMemoryDataPath()
          .setLogicalName(source.getLogicalName())
          .createRelationDef()
          .copyDataDef(source);
        for (DataPath templateDataPath : templateModels
          .stream()
          .map(TemplateModel::getTemplateOriginDataPath)
          .filter(Objects::nonNull)
          .collect(Collectors.toList())
        ) {

          String columnName = getOutputLogicalNameForTemplate(envVariables, envVariablesDataPathAttributes, templateDataPath);
          enrichOutputDef.addColumn(columnName);

        }
        DataPath enrichDataPath = enrichOutputDef.getDataPath();
        enrichInsertStream = enrichDataPath.getInsertStream();
        this.outputs.add(enrichDataPath);

      }


      /**
       * For each record
       */
      try (SelectStream selectStream = source.getSelectStream()) {
        while (selectStream.next()) {

          /**
           * For each template
           */
          for (TemplateModel templateModel : templateModels) {

            DataPath templateDataPath = templateModel.getTemplateOriginDataPath();
            if (templateDataPath != null) {
              for (DataPathAttribute envVariablesDataPathAttribute : envVariablesDataPathAttributes) {
                try {
                  envVariables.put(TEMPLATE_PREFIX + Key.toLongOptionName(envVariablesDataPathAttribute), templateDataPath.getVariable(envVariablesDataPathAttribute));
                } catch (NoVariableException ex) {
                  // ok
                }
              }
            }

            Template template = this.createTemplateEngineFromProperties(templateModel);

            /**
             * Apply the scalar variables
             */
            Map<String, Object> values = new HashMap<>();
            for (ColumnDef columnDef : source.getOrCreateRelationDef().getColumnDefs()) {
              values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
            }
            template.applyVariables(values);

            /**
             * Add tabular variables from the template-tables
             */
            Map<String, DataUri> tabularVariables = this.templateStep.tableVariables;
            if (!tabularVariables.isEmpty()) {
              Map<String, Object> tablesTemplateVariables = new HashMap<>();
              for (Map.Entry<String, DataUri> tablesVariable : tabularVariables.entrySet()) {
                String variableName = tablesVariable.getKey();
                DataUri dataUri = tablesVariable.getValue();
                List<Map<String, Object>> records = new ArrayList<>();
                List<DataPath> dataPaths = this.templateStep.getTabular().select(dataUri);
                for (DataPath tableDataPath : dataPaths) {
                  try (SelectStream variableTableSelectStream = tableDataPath.getSelectStream()) {
                    while (variableTableSelectStream.next()) {
                      HashMap<String, Object> record = new HashMap<>();
                      for (ColumnDef columnDef : tableDataPath.getOrCreateRelationDef().getColumnDefs()) {
                        record.put(columnDef.getColumnName(), variableTableSelectStream.getString(columnDef.getColumnPosition()));
                      }
                      records.add(record);
                    }
                  } catch (SelectException e) {
                    throw new RuntimeException(e);
                  }
                }
                tablesTemplateVariables.put(variableName, records);
              }
              template.applyVariables(tablesTemplateVariables);
            }


            String result = template.getResult();
            result = this.applyEmailCssInlineIfNeeded(result, templateModel);

            switch (this.templateStep.templateOutputOperation) {
              case TEMPLATES:
                String targetLogicalName = templateModel.getLogicalName();
                if (templateDataPath != null) {
                  targetLogicalName = getOutputLogicalNameForTemplate(envVariables, envVariablesDataPathAttributes, templateDataPath);
                }
                DataPath outputDataPath = tabular.getAndCreateRandomMemoryDataPath()
                  .setLogicalName(targetLogicalName)
                  .createRelationDef()
                  .addColumn("output")
                  .getDataPath();
                outputDataPath
                  .getInsertStream()
                  .insert(result)
                  .close();
                this.outputs.add(outputDataPath);
                break;
              case EXTENDED_RECORDS:
                enrichedTemplateResults.add(result);
                break;
              default:
                throw new IllegalStateException("Internal Error: The template output operation " + this.templateStep.templateOutputOperation + " was not implemented");
            }

          }


          List<Object> objects = selectStream.getObjects()
            .stream()
            .map(e -> (Object) e)
            .collect(Collectors.toList());
          objects.addAll(enrichedTemplateResults);
          enrichedTemplateResults = new ArrayList<>();
          if (enrichInsertStream != null) {
            enrichInsertStream.insert(objects);
          }

        }

      } catch (SelectException e) {
        throw new RuntimeException(e);
      } finally {
        if (enrichInsertStream != null) {
          enrichInsertStream.close();
        }
      }
    }

    private String applyEmailCssInlineIfNeeded(String result, TemplateModel templateProperties) {
      if (!templateProperties.getMediaType().equals(MediaTypes.TEXT_HTML)) {
        return result;
      }
      if (!TemplateStep.this.isTemplateEmail) {
        return result;
      }
      return CssInliner
        .createFromStringDocument(result)
        .inline()
        .toString();
    }

    /**
     * When the template is a hierarchical structure.
     * The template is an accumulator and run on the resource level
     */
    private void runResourceLevel(
      Set<TemplateModel> templates,
      Map<String, Object> envVariables,
      DataPath source
    ) throws SelectException {


      /**
       * For each template
       */
      for (TemplateModel template : templates) {

        DataPath templateOriginDataPath = template.getTemplateOriginDataPath();
        if (templateOriginDataPath != null) {
          for (DataPathAttribute envVariablesDataPathAttribute : TemplateStep.this.dataPathAttributesAddedAsEnv) {
            try {
              envVariables.put("template-" + Key.toLongOptionName(envVariablesDataPathAttribute), templateOriginDataPath.getVariable(envVariablesDataPathAttribute).getValueOrDefault());
            } catch (NoVariableException | NoValueException ex) {
              // ok
            }
          }
        }


        Template templateEngine = this.createTemplateEngineFromProperties(template);

        /**
         * All data are applied to the same template
         * creating a hierarchical output
         */
        try (SelectStream selectStream = source.getSelectStream()) {
          while (selectStream.next()) {
            Map<String, Object> values = new HashMap<>();
            for (ColumnDef columnDef : source.getOrCreateRelationDef().getColumnDefs()) {
              values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
            }
            templateEngine.applyVariables(values);
          }
        } catch (SelectException e) {
          throw new RuntimeException(e);
        }

        String outputLogicalName = this.templateStep.getOutputLogicalName();
        String logicalName = "output";
        if (outputLogicalName != null) {
          logicalName = TextTemplateEngine
            .getOrCreate()
            .compile(outputLogicalName)
            .applyVariables(envVariables)
            .getResult();
        }


        DataPath outputDataPath = tabular.getAndCreateRandomMemoryDataPath()
          .setLogicalName(logicalName)
          .createRelationDef()
          .addColumn(template.getMediaType().getSubType())
          .getDataPath();

        String result = templateEngine.getResult();
        result = this.applyEmailCssInlineIfNeeded(result, template);

        outputDataPath
          .getInsertStream()
          .insert(result)
          .close();
        this.outputs.add(outputDataPath);

      }

    }


    private String getOutputLogicalNameForTemplate
      (Map<String, Object> envVariables, List<DataPathAttribute> envVariablesDataPathAttributes, DataPath
        templateDataPath) {
      String columnName = templateDataPath.getLogicalName();
      String outputLogicalOutput = this.templateStep.getOutputLogicalName();
      if (outputLogicalOutput != null) {
        for (DataPathAttribute e : envVariablesDataPathAttributes) {
          try {
            envVariables.put(TEMPLATE_PREFIX + Key.toLongOptionName(e), templateDataPath.getVariable(e).getValueOrDefault());
          } catch (NoVariableException | NoValueException ex) {
            // ok
          }
        }
        columnName = TextTemplateEngine
          .getOrCreate()
          .compile(outputLogicalOutput)
          .applyVariables(envVariables)
          .getResult();
      }
      return columnName;
    }

    private Template createTemplateEngineFromProperties(TemplateModel templateProperties) {
      switch (templateEngine) {
        case NATIVE:
          String normalizedExtension = templateProperties.getMediaType().getExtension();
          switch (normalizedExtension) {
            case "json":
              return JsonTemplate.compile(templateProperties.getContent());
            case "txt":
              return TextTemplateEngine.getOrCreate().compile(templateProperties.getContent());
            default:
              throw new IllegalArgumentException("The native engine does not support the template in " + normalizedExtension);
          }
        case THYMELEAF:
          return ThymeleafTemplateEngine.getOrCreate().compile(templateProperties.getContent());
        default:
          throw new IllegalArgumentException("The engine " + templateEngine + " is not yet implemented");
      }
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
      return this.outputs;
    }

    @Override
    public Set<DataPath> get(long timeout, @SuppressWarnings("NullableProblems") TimeUnit unit) {
      return get();
    }

  }

  private String getOutputLogicalName() {
    return this.outputLogicalName;
  }

  @Override
  public TemplateStep setArguments(MapKeyIndependent<Object> arguments) {

    for (Map.Entry<String, Object> entry : arguments.entrySet()) {
      Object value = entry.getValue();
      if (value == null) {
        throw new IllegalStateException("The value of the property (" + entry.getKey() + ") of the step (" + this + ") is null.");
      }
      TemplateAttributes s;
      try {
        s = Casts.cast(entry.getKey(), TemplateAttributes.class);
      } catch (CastException e) {
        throw IllegalArgumentExceptions.createForStepArgument(entry.getKey(), this.toString(), TemplateAttributes.class, e);
      }
      switch (s) {
        case TEMPLATE_INLINE:
          MapKeyIndependent<String> mapInlineTemplate = YamlCast.castToMapKeyIndependent(value, String.class);
          String inLineTemplateContent = null;
          String inlineTemplateLogicalName = null;
          MediaType type = null;
          for (Map.Entry<String, String> entryInlineTemplate : mapInlineTemplate.entrySet()) {
            switch (Key.toNormalizedKey(entryInlineTemplate.getKey())) {
              case "type":
              case "datatype":
              case "mimetype":
                try {
                  type = MediaTypes.createFromMediaTypeString(entryInlineTemplate.getValue());
                } catch (NullValueException e) {
                  throw IllegalArgumentExceptions.createFromMessage("The media type value was null for the template step (" + this + ")");
                }
                break;
              case "content":
              case "text":
                inLineTemplateContent = entryInlineTemplate.getValue();
                break;
              case "logicalname":
                inlineTemplateLogicalName = entryInlineTemplate.getValue();
                break;
              default:
                throw new IllegalStateException("The inline template property `" + entryInlineTemplate.getKey() + "` of the step (" + this + ") is unknown.");
            }
          }
          if (inLineTemplateContent == null) {
            throw new IllegalStateException("The inline template of the step (" + this + ") does not have any `content` property. We can't therefore define a template");
          }
          if (type == null) {
            throw new IllegalStateException("The type of the inline template could not be found");
          }
          if (inlineTemplateLogicalName == null) {
            inlineTemplateLogicalName = Digest.createFromString(Digest.Algorithm.MD5, inLineTemplateContent).getHashHex();
          }
          TemplateModel templateProperties = TemplateModel
            .create()
            .setMediaType(type)
            .setContent(inLineTemplateContent);
          this.inlineTemplates.put(inlineTemplateLogicalName, templateProperties);
          break;
        case STEP_OUTPUT_LOGICAL_NAME:
          if (!(value instanceof String)) {
            throw new IllegalStateException("The output logical name attribute is not a string but a " + value.getClass().getSimpleName());
          }
          this.setOutputLogicalName((String) value);
          break;
        case TEMPLATE_SELECTORS:
        case TEMPLATE_SELECTOR:
          Collection<?> dataSelectors;
          if (value instanceof Collection) {
            dataSelectors = ((Collection<?>) value);
          } else {
            dataSelectors = Collections.singletonList(value);
          }
          for (Object dataSelector : dataSelectors) {
            this.templateSelectors.add(tabular.createDataUri(dataSelector.toString()));
          }
          break;
        case STEP_GRANULARITY:
          try {
            this.granularity = Casts.cast(value.toString(), Granularity.class);
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createForArgumentValueForStep(value.toString(), STEP_GRANULARITY, this.toString(), Granularity.class, e);
          }
          break;
        case TEMPLATE_TABULAR_VARIABLES:
          if (!(value instanceof List)) {
            throw new IllegalStateException("The table variables attribute is not a list but a " + value.getClass().getSimpleName());
          }
          List<Object> tableVariables = Casts.castToListSafe(value, Object.class);
          for (Object tableVariable : tableVariables) {
            if (!(tableVariable instanceof Map)) {
              throw new IllegalStateException("A table variable value is not a map but a " + value.getClass().getSimpleName());
            }
            Map<String, String> tableVariableMap;
            try {
              tableVariableMap = Casts.castToSameMap(tableVariable, String.class, String.class);
            } catch (CastException e) {
              throw new InternalException("String, string should not throw an exception", e);
            }
            String variableName = tableVariableMap.get("name");
            String variableDataSelectors = tableVariableMap.get("selectors");
            this.addTemplateTabularVariable(variableName, tabular.createDataUri(variableDataSelectors));
          }
          break;
        case STEP_OUTPUT:
          if (!(value instanceof String)) {
            throw new IllegalStateException("The output operation is not a string but a " + value.getClass().getSimpleName());
          }
          try {
            this.setOutputOperation(Casts.cast(value.toString(), TemplateOutputOperation.class));
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createForArgumentValueForStep(value.toString(), STEP_OUTPUT, this.toString(), TemplateOutputOperation.class, e);
          }
          break;
        case TEMPLATE_EMAIL:
          Boolean b = Booleans.createFromString(value.toString()).toBoolean();
          this.setIsTemplateEmail(b);
          break;
        default:
          throw new IllegalStateException("Internal error: the attribute (" + entry.getKey() + ") of the step (" + this + ") was not process");
      }
    }

    // Mandatory
    if (this.templateSelectors.isEmpty() && this.inlineTemplates.isEmpty()) {
      throw new IllegalStateException("A template is mandatory (selector or inline) for the the step (" + this + ")");
    }
    return this;
  }

  private TemplateStep setIsTemplateEmail(Boolean b) {
    this.isTemplateEmail = b;
    return this;
  }

  public TemplateStep setOutputOperation(TemplateOutputOperation templateOutputOperation) {
    this.templateOutputOperation = templateOutputOperation;
    return this;
  }

  /**
   * The memory representation of a template
   */
  static class TemplateModel {

    private MediaType type;
    private String content;
    private String logicalName = "default-template-name";
    /**
     * The origin of the template, if null, this is an inline data path
     */
    private DataPath templateOriginDataPath;

    static TemplateModel create() {
      return new TemplateModel();
    }

    public static TemplateModel createFromDataPath(DataPath templateDataPath) {
      TemplateModel templateProperties = new TemplateModel();
      if (templateDataPath instanceof FsDataPath) {
        FsDataPath templateFsDataPath = (FsDataPath) templateDataPath;
        Path targetPath = templateFsDataPath.getAbsoluteNioPath();
        String jsonStringTemplate = Strings.createFromPath(targetPath).toString();
        templateProperties.setMediaType(templateDataPath.getMediaType())
          .setContent(jsonStringTemplate)
          .setLogicalName(templateDataPath.getLogicalName())
          .setDataPath(templateDataPath);
      } else {
        throw new UnsupportedOperationException("The template data resource (" + templateDataPath + ") is not a file and this is not yet supported");
      }
      return templateProperties;
    }

    private TemplateModel setDataPath(DataPath templateDataPath) {
      this.templateOriginDataPath = templateDataPath;
      return this;
    }


    private TemplateModel setLogicalName(String s) {
      this.logicalName = s;
      return this;
    }

    private TemplateModel setContent(String content) {
      this.content = content;
      return this;
    }

    public TemplateModel setMediaType(MediaType type) {
      this.type = type;
      return this;
    }

    public MediaType getMediaType() {
      return this.type;
    }

    public String getContent() {
      return this.content;
    }

    public String getLogicalName() {
      return this.logicalName;
    }

    public DataPath getTemplateOriginDataPath() {
      return this.templateOriginDataPath;
    }
  }
}
