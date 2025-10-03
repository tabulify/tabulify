package net.bytle.template.flow;

import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepIntermediateOneToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.flow.operation.DefinePipelineStep;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.spi.MetaMap;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.template.TemplateMetas;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.InternalException;
import net.bytle.html.CssInliner;
import net.bytle.template.JsonTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.template.ThymeleafTemplateEngine;
import net.bytle.template.api.Template;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.template.flow.TemplateEngine.NATIVE;
import static net.bytle.template.flow.TemplateEngine.THYMELEAF;
import static net.bytle.template.flow.TemplatePipelineStep.TEMPLATE;
import static net.bytle.type.MediaTypeExtension.JSON_EXTENSION;
import static net.bytle.type.MediaTypeExtension.TEXT_EXTENSION;


/**
 * A multiply operation because
 * from one data path, we can create multiply files
 * from multiple templates
 */
public class TemplatePipelineStepStream extends PipelineStepIntermediateOneToManyAbs implements PipelineStep {


  private final TemplatePipelineStep templateBuilder;
  private HashSet<TemplateModel> templateModels;

  public TemplatePipelineStepStream(TemplatePipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.templateBuilder = pipelineStepBuilder;
  }


  @Override
  public void onStart() {

    templateModels = new HashSet<>(templateBuilder.inlineTemplates.values());

    /**
     * Template Selectors if any
     */
    List<DataPath> templateDataPaths;
    List<DataUriNode> templateSelectors = templateBuilder.templateSelectors;
    if (!templateSelectors.isEmpty()) {
      templateDataPaths = getTabular().select(templateSelectors, false, null);
      if (templateDataPaths.isEmpty()) {
        throw new RuntimeException("No templates were selected by the selectors (" + templateSelectors.stream().map(DataUriNode::toString).collect(Collectors.joining(",")));
      }
      for (DataPath templateDataPath : templateDataPaths) {

        templateModels.add(TemplateModel.createFromDataPath(templateDataPath));
      }
    }

    /**
     * Flow Granularity and engine definition
     */
    for (TemplateModel templateModel : templateModels) {


      MediaType mediaType = templateModel.getMediaType();

      /**
       * Default template engine and execution
       */
      switch (mediaType.getExtension()) {
        case JSON_EXTENSION:
          // hierarchical structure
          if (templateBuilder.templateEngine == null) {
            templateBuilder.templateEngine = NATIVE;
          }
          if (templateBuilder.granularity == null) {
            templateBuilder.granularity = Granularity.RESOURCE;
          }
          if (templateBuilder.processingType == null) {
            templateBuilder.processingType = ProcessingType.CUMULATIVE;
          }
          break;
        case TEXT_EXTENSION:
          if (templateBuilder.processingType == ProcessingType.CUMULATIVE) {
            throw new RuntimeException("You cannot mixed a hierarchical template (json) with a non-hierarchical template (txt) in the same step");
          }
          if (templateBuilder.templateEngine == null) {
            templateBuilder.templateEngine = NATIVE;
          }
          if (templateBuilder.granularity == null) {
            templateBuilder.granularity = Granularity.RECORD;
          }
          break;
        default:
          if (templateBuilder.processingType == ProcessingType.CUMULATIVE) {
            throw new RuntimeException("You cannot mixed a hierarchical template (json) with a non-hierarchical template (" + templateModel.getMediaType().getSubType() + ") in the same step");
          }
          if (templateBuilder.templateEngine == null) {
            templateBuilder.templateEngine = THYMELEAF;
          }
          if (templateBuilder.granularity == null) {
            templateBuilder.granularity = Granularity.RECORD;
          }
      }
    }
  }

  @Override
  public PipelineStepSupplierDataPath apply(DataPath input) {


    List<DataPath> dataPaths;
    switch (templateBuilder.granularity) {
      case RESOURCE:
        if (templateBuilder.processingType != ProcessingType.CUMULATIVE) {
          throw new RuntimeException("A map resource processing type is not yet implemented");
        }
        try {
          dataPaths = this.runCumulativeResourceExecutionMode(
            templateModels,
            input
          );
        } catch (SelectException e) {
          throw new RuntimeException("Error while running the template step on a resource level (ie hierarchical templating) (" + this + ". Error:" + e.getMessage());
        }
        break;
      case RECORD:
        dataPaths = this.runRecordLevel(
          templateModels,
          input
        );
        break;
      default:
        throw new InternalException("The granularity: " + templateBuilder.granularity + " should have been implemented");
    }

    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPaths(dataPaths)
      .setIntermediateSupplier(this)
      .build();


  }


  private List<DataPath> runRecordLevel(
    Set<TemplateModel> templateModels,
    DataPath input
  ) {

    List<DataPath> outputs = new ArrayList<>();

    /**
     * If the output is the extended format, we create
     * a copy of the source and add a column by template
     */
    InsertStream enrichInsertStream = null;
    List<String> enrichedTemplateResults = new ArrayList<>();
    if (templateBuilder.templateTargetType == TemplateTargetType.ENRICHED_INPUT) {

      RelationDef enrichOutputDef = input.getConnection().getTabular().getAndCreateRandomMemoryDataPath()
        .setLogicalName(input.getLogicalName())
        .createRelationDef()
        .copyDataDef(input);
      for (TemplateModel templateDataPath : templateModels) {
        String columnName = this.templateBuilder.targetColumnNameTemplateFunction.apply(
          this.getTemplateVariableBuilder(input, templateDataPath)
        );
        enrichOutputDef.addColumn(columnName);
      }
      DataPath enrichDataPath = enrichOutputDef.getDataPath();
      enrichInsertStream = enrichDataPath.getInsertStream();
      outputs.add(enrichDataPath);

    }


    /**
     * For each record
     */
    try (SelectStream selectStream = input.getSelectStream()) {
      while (selectStream.next()) {

        /**
         * For each template
         */
        for (TemplateModel templateModel : templateModels) {


          Template template = this.createTemplateEngineFromProperties(templateModel);

          /**
           * Apply the scalar variables
           */
          Map<String, Object> values = new HashMap<>();
          for (ColumnDef columnDef : input.getOrCreateRelationDef().getColumnDefs()) {
            values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
          }
          template.applyVariables(values);

          /**
           * Add tabular variables from the template-tables
           */
          Map<String, DataUriNode> modelVariables = templateBuilder.modelVariables;
          if (!modelVariables.isEmpty()) {
            Map<String, Object> tablesTemplateVariables = new HashMap<>();
            for (Map.Entry<String, DataUriNode> tablesVariable : modelVariables.entrySet()) {
              String variableName = tablesVariable.getKey();
              DataUriNode dataUri = tablesVariable.getValue();
              List<Map<String, Object>> records = new ArrayList<>();
              List<DataPath> dataPaths = input.getConnection().getTabular().select(dataUri);
              for (DataPath tableDataPath : dataPaths) {
                try (SelectStream variableTableSelectStream = tableDataPath.getSelectStream()) {
                  while (variableTableSelectStream.next()) {
                    HashMap<String, Object> record = new HashMap<>();
                    for (ColumnDef columnDef : tableDataPath.getOrCreateRelationDef().getColumnDefs()) {
                      Integer columnPosition = columnDef.getColumnPosition();
                      record.put(columnDef.getColumnName(), variableTableSelectStream.getString(columnPosition));
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

          switch (templateBuilder.templateTargetType) {
            case TEMPLATE_OUTPUT:
              String targetLogicalName = templateModel.getLogicalName();

              TemplateMetas templateMetas = getTemplateVariableBuilder(input, templateModel);
              String targetColumnName = this.templateBuilder.targetColumnNameTemplateFunction.apply(templateMetas);
              DataPath targetDataPath = this.templateBuilder.getTargetUriFunction().apply(input, templateMetas);
              DataPath outputDataPath = targetDataPath
                .setLogicalName(targetLogicalName)
                .createRelationDef()
                .addColumn(targetColumnName)
                .getDataPath();
              outputDataPath
                .getInsertStream()
                .insert(result)
                .close();
              outputs.add(outputDataPath);
              break;
            case ENRICHED_INPUT:
              enrichedTemplateResults.add(result);
              break;
            default:
              throw new InternalException("The template output operation " + templateBuilder.templateTargetType + " was not in the switch branch");
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
    return outputs;

  }

  private String applyEmailCssInlineIfNeeded(String result, TemplateModel templateProperties) {
    if (!templateProperties.getMediaType().equals(MediaTypes.TEXT_HTML)) {
      return result;
    }
    if (!templateBuilder.isTemplateEmail) {
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
   *
   * @return data paths (one by template)
   */
  private List<DataPath> runCumulativeResourceExecutionMode(
    Set<TemplateModel> templates,
    DataPath input
  ) throws SelectException {


    /**
     * For each template
     */
    List<DataPath> returns = new ArrayList<>();


    for (TemplateModel template : templates) {


      Template templateEngine = this.createTemplateEngineFromProperties(template);

      /**
       * All data are applied to the same template
       * creating a hierarchical output
       */
      try (SelectStream selectStream = input.getSelectStream()) {
        while (selectStream.next()) {
          Map<String, Object> values = new HashMap<>();
          for (ColumnDef columnDef : input.getOrCreateRelationDef().getColumnDefs()) {
            values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
          }
          templateEngine.applyVariables(values);
        }
      } catch (SelectException e) {
        throw new RuntimeException(e);
      }

      /**
       * Template Data Uri variable
       * The variables used by the template target uri
       */
      DataPath targetDataPath = templateBuilder.getTargetUriFunction().apply(
          input,
          getTemplateVariableBuilder(input, template)
        )
        .createEmptyRelationDef()
        .addColumn(template.getMediaType().getSubType())
        .getDataPath();

      String result = templateEngine.getResult();
      result = this.applyEmailCssInlineIfNeeded(result, template);

      targetDataPath
        .getInsertStream()
        .insert(result)
        .close();
      returns.add(targetDataPath);

    }

    return returns;
  }

  private TemplateMetas getTemplateVariableBuilder(DataPath input, TemplateModel template) {
    TemplateMetas builder = TemplateMetas.builder()
      .addInputDataPath(input);

    DataPath templateOriginDataPath = template.getTemplateOriginDataPath();
    if (templateOriginDataPath != null) {

      builder.addMeta(templateOriginDataPath, TemplatePipelineStep.TEMPLATE_PREFIX);

    } else {

      /**
       * Inline template
       */
      MetaMap inlineVariables = new MetaMap(this.getTabular(), template.getLogicalName());
      KeyNormalizer name = KeyNormalizer.createSafe(DataPathAttribute.NAME);
      inlineVariables.put(name, template.getLogicalName() + "." + template.getMediaType().getExtension());
      KeyNormalizer templateLogicalName = KeyNormalizer.createSafe(DataPathAttribute.LOGICAL_NAME);
      inlineVariables.put(templateLogicalName, template.getLogicalName());
      KeyNormalizer mediaType = KeyNormalizer.createSafe(DataPathAttribute.MEDIA_TYPE);
      inlineVariables.put(mediaType, template.getMediaType());
      KeyNormalizer mediaSubType = KeyNormalizer.createSafe(DataPathAttribute.MEDIA_SUBTYPE);
      inlineVariables.put(mediaSubType, template.getMediaType().getSubType());
      builder.addMetaMap(inlineVariables, TEMPLATE);

    }
    return builder;
  }


  private Template createTemplateEngineFromProperties(TemplateModel templateProperties) {
    switch (templateBuilder.templateEngine) {
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
        throw new IllegalArgumentException("The engine " + templateBuilder.templateEngine + " is not yet implemented");
    }
  }


}
