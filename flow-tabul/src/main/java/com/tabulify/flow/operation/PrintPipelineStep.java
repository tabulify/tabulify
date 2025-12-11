package com.tabulify.flow.operation;


import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMap;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.flow.stream.PipelinePeekOperation;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.Printer;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.List;

/**
 * Print a data path content
 */
public class PrintPipelineStep extends PipelineStepIntermediateMapAbs implements PipelineStepIntermediateMap, PipelinePeekOperation {


  private final Printer printer;

  public PrintPipelineStep(PrintPipelineStepBuilder pipelineStepBuilder) {

    super(pipelineStepBuilder);

    this.printer = Printer.builder()
      .setFormat(pipelineStepBuilder.format)
      .setPrintNonVisibleCharacter(pipelineStepBuilder.printNonVisibleCharacters)
      .setPrintColumnHeaders(pipelineStepBuilder.printColumnHeaders)
      .setPrintTableHeader(pipelineStepBuilder.printTableHeader)
      .setFooterSeparationLineCount(pipelineStepBuilder.footerSeparationLineCount)
      .setColorsColumnName(pipelineStepBuilder.colorsColumnName)
      .setBooleanTrueToken(pipelineStepBuilder.booleanTrueToken)
      .setBooleanFalseToken(pipelineStepBuilder.booleanFalseToken)
      .setNullToken(pipelineStepBuilder.nullToken)
      .setEmptyStringToken(pipelineStepBuilder.emptyStringToken)
      .setBlankStringToken(pipelineStepBuilder.blankStringToken)
      .build();
  }

  public static PrintPipelineStepBuilder builder() {
    return new PrintPipelineStepBuilder();
  }


  /**
   * The map interface (ie return)
   *
   * @param dataPath the function argument
   */
  @Override
  public DataPath apply(DataPath dataPath) {
    accept(dataPath);
    return dataPath;
  }

  /**
   * The consumer interface (ie no return)
   *
   * @param sourceDataPath the input argument
   */
  @Override
  public void accept(DataPath sourceDataPath) {
    printer.print(sourceDataPath);
  }

  public static class PrintPipelineStepBuilder extends PipelineStepBuilder {

    private static final KeyNormalizer PRINT = KeyNormalizer.createSafe("print");
    public String nullToken = (String) PrintPipelineStepArgument.NULL_TOKEN.getDefaultValue();
    public String emptyStringToken = (String) PrintPipelineStepArgument.STRING_EMPTY_TOKEN.getDefaultValue();
    public String blankStringToken = (String) PrintPipelineStepArgument.STRING_BLANK_TOKEN.getDefaultValue();

    private boolean printNonVisibleCharacters = (boolean) PrintPipelineStepArgument.PRINT_NON_VISIBLE_CHARACTERS.getDefaultValue();
    private boolean printColumnHeaders = (boolean) PrintPipelineStepArgument.PRINT_COLUMN_HEADERS.getDefaultValue();
    private boolean printTableHeader = (boolean) PrintPipelineStepArgument.PRINT_TABLE_HEADER.getDefaultValue();
    private PrinterPrintFormat format = (PrinterPrintFormat) PrintPipelineStepArgument.FORMAT.getDefaultValue();
    private Integer footerSeparationLineCount = (Integer) PrintPipelineStepArgument.FOOTER_SEPARATION_LINE_COUNT.getDefaultValue();
    private String colorsColumnName = (String) PrintPipelineStepArgument.COLORS_COLUMN_NAME.getDefaultValue();
    private String booleanTrueToken = (String) PrintPipelineStepArgument.BOOLEAN_TRUE_TOKEN.getDefaultValue();
    private String booleanFalseToken = (String) PrintPipelineStepArgument.BOOLEAN_FALSE_TOKEN.getDefaultValue();

    @Override
    public PrintPipelineStepBuilder createStepBuilder() {
      return new PrintPipelineStepBuilder();
    }

    public PrintPipelineStepBuilder setPrintNonVisibleCharacters(boolean printNonVisibleCharacter) {
      this.printNonVisibleCharacters = printNonVisibleCharacter;
      return this;
    }

    public PrintPipelineStepBuilder setPrintColumnHeaders(boolean printColumnHeaders) {
      this.printColumnHeaders = printColumnHeaders;
      return this;
    }

    public PrintPipelineStepBuilder setPrintTableHeader(boolean printTableHeader) {
      this.printTableHeader = printTableHeader;
      return this;
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      return List.of(PrintPipelineStepArgument.class);
    }


    @Override
    public PrintPipelineStepBuilder setArgument(KeyNormalizer key, Object value) {


      PrintPipelineStepArgument printArgument;
      try {
        printArgument = Casts.cast(key, PrintPipelineStepArgument.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ListPipelineStepArgument.class));
      }
      Attribute attribute;
      try {
        attribute = this.getPipeline().getTabular().getVault()
          .createVariableBuilderFromAttribute(printArgument)
          .setOrigin(Origin.PIPELINE)
          .build(value);
        this.setArgument(attribute);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + printArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
      }

      switch (printArgument) {
        case FORMAT:
          try {
            this.setFormat(attribute.getValueOrDefaultCastAs(PrinterPrintFormat.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The " + printArgument + " value (" + value + ") of the step (" + this + ") is not conform . You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(PrinterPrintFormat.class), e);
          }
          break;
        case PRINT_TABLE_HEADER:
          this.setPrintTableHeader((Boolean) attribute.getValueOrDefault());
          break;
        case PRINT_COLUMN_HEADERS:
          this.setPrintColumnHeaders((Boolean) attribute.getValueOrDefault());
          break;
        case PRINT_NON_VISIBLE_CHARACTERS:
          this.setPrintNonVisibleCharacters((Boolean) attribute.getValueOrDefault());
          break;
        case FOOTER_SEPARATION_LINE_COUNT:
          this.setFooterSeparationLineCount((Integer) attribute.getValueOrDefault());
          break;
        case COLORS_COLUMN_NAME:
          this.setColorsColumnName((String) attribute.getValueOrDefault());
          break;
        case BOOLEAN_TRUE_TOKEN:
          this.setBooleanTrueToken((String) attribute.getValueOrDefault());
          break;
        case BOOLEAN_FALSE_TOKEN:
          this.setBooleanFalseToken((String) attribute.getValueOrDefault());
          break;
        case NULL_TOKEN:
          this.setNullToken((String) attribute.getValueOrDefault());
          break;
        case STRING_BLANK_TOKEN:
          this.setBlankToken((String) attribute.getValueOrDefault());
          break;
        case STRING_EMPTY_TOKEN:
          this.setEmptyToken((String) attribute.getValueOrDefault());
          break;
        default:
          throw new InternalException("The " + printArgument + " value (" + value + ") of the step (" + this + ") was not processed");
      }
      return this;
    }

    private PrintPipelineStepBuilder setEmptyToken(String emptyToken) {
      this.emptyStringToken = emptyToken;
      return this;
    }

    private PrintPipelineStepBuilder setBlankToken(String blankToken) {
      this.blankStringToken = blankToken;
      return this;
    }

    private PrintPipelineStepBuilder setNullToken(String nullToken) {
      this.nullToken = nullToken;
      return this;
    }

    private PrintPipelineStepBuilder setBooleanFalseToken(String s) {
      this.booleanFalseToken = s;
      return this;
    }

    private PrintPipelineStepBuilder setBooleanTrueToken(String s) {
      this.booleanTrueToken = s;
      return this;
    }

    public PrintPipelineStepBuilder setColorsColumnName(String colorsColumnName) {
      this.colorsColumnName = colorsColumnName;
      return this;
    }

    public PrintPipelineStepBuilder setFooterSeparationLineCount(Integer separationLineCount) {
      this.footerSeparationLineCount = separationLineCount;
      return this;
    }

    public PrintPipelineStepBuilder setFormat(PrinterPrintFormat operationMode) {
      this.format = operationMode;
      return this;
    }


    @Override
    public PrintPipelineStep build() {
      if (this.format == null && this.getPipeline().getProcessingType() == PipelineStepProcessingType.STREAM) {
        this.format = PrinterPrintFormat.STREAM;
      }
      return new PrintPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return PRINT;
    }

  }
}
