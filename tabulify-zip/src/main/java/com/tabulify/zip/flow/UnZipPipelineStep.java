package com.tabulify.zip.flow;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.flow.operation.StreamType;
import com.tabulify.fs.FsDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.spi.MetaMap;
import com.tabulify.template.TemplateMetas;
import com.tabulify.zip.api.ArchiveEntry;
import com.tabulify.zip.api.ArchiveEntryAttribute;
import com.tabulify.zip.api.ArchiveIterator;
import com.tabulify.zip.datapath.ArchiveDataPath;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoPathFoundException;
import net.bytle.fs.Fs;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tabulify.zip.flow.UnZipPipelineStepArgument.TARGET_DATA_URI;

public class UnZipPipelineStep extends PipelineStepBuilderTarget {

  static final KeyNormalizer UNZIP = KeyNormalizer.createSafe("unzip");
  public static final KeyNormalizer TEMPLATE_ENTRY_PREFIX = KeyNormalizer.createSafe("entry");

  public Glob entrySelector = (Glob) UnZipPipelineStepArgument.ENTRY_SELECTOR.getDefaultValue();
  private StepOutputArgument output = (StepOutputArgument) UnZipPipelineStepArgument.OUTPUT_TYPE.getDefaultValue();
  private StreamType streamType = (StreamType) UnZipPipelineStepArgument.STREAM_TYPE.getDefaultValue();
  private Integer stripComponents = (Integer) UnZipPipelineStepArgument.STRIP_COMPONENTS.getDefaultValue();


  public static UnZipPipelineStep builder() {
    return new UnZipPipelineStep();
  }


  public UnZipPipelineStep setEntrySelector(Glob entrySelector) {
    this.entrySelector = entrySelector;
    return this;
  }

  @Override
  public UnZipPipelineStep createStepBuilder() {
    return new UnZipPipelineStep();
  }


  @Override
  public PipelineStep build() {

    /**
     * Target
     */
    this.setTargetTemplateExtraPrefixes(Set.of(TEMPLATE_ENTRY_PREFIX));
    if (this.getTargetUri() == null) {
      this.setTargetDataUri(
        this.getTabular().createDataUri(TARGET_DATA_URI.getDefaultValue().toString())
      );
    }
    try {
      this.getTargetUri().getPath();
    } catch (NoPathFoundException e) {
      throw new IllegalArgumentException("The target data uri should have a path as it must be unique for each entry. The actual target data uri value is: " + this.getTargetUri(), e);
    }

    switch (output) {
      case INPUTS:
      case RESULTS:
        switch (streamType) {
          case MAP:
            return new UnZipPipelineStepIntermediateMap(this);
          case SPLIT:
            return new UnZipPipelineStepIntermediateOneToMany(this);
          default:
            throw new InternalError("The stream type " + streamType + " was not implemented in the switch");
        }
      case TARGETS:
        return new UnZipPipelineStepIntermediateOneToMany(this);
      default:
        throw new InternalError("The output type " + output + " was not implemented in the switch");
    }

  }

  public UnZipPipelineStep setOutput(StepOutputArgument stepOutputArgument) {
    this.output = stepOutputArgument;
    return this;
  }


  @Override
  public KeyNormalizer getOperationName() {
    return UNZIP;
  }

  public StepOutputArgument getOutputType() {
    return this.output;
  }

  public ArchiveDataPath checkIfArchive(DataPath inputDataPath) {
    if (!(inputDataPath instanceof ArchiveDataPath)) {
      throw new IllegalArgumentException("The input data resource (" + inputDataPath + ") is not an archive but a " + inputDataPath.getMediaType());
    }
    return (ArchiveDataPath) inputDataPath;
  }

  /**
   * Private because the code should call {@link #getTargetPath(ArchiveDataPath, ArchiveEntry)}
   */
  FsDataPath getTargetPath(ArchiveDataPath archiveDataPath, ArchiveEntry entry) {

    String entryName = this.getEntryName(entry);
    /**
     * Build the meta for the target template function
     */
    MetaMap metaMap = new MetaMap(this.getTabular());
    metaMap.put(DataPathAttribute.PATH.toKeyNormalizer(), entryName);
    // Add matched group from entry selectors
    // We start from 0 for backwards compatibility, ($0) being the whole name
    int start = 0;
    List<String> matchedGroups = entry.getMatchedGroups();
    for (int i = start; i < matchedGroups.size(); i++) {
      KeyNormalizer key = KeyNormalizer.createSafe(i);
      String value = matchedGroups.get(i);
      metaMap.put(key, value);
    }
    TemplateMetas templateMetas = TemplateMetas.builder()
      .addMetaMap(metaMap, TEMPLATE_ENTRY_PREFIX)
      .addInputDataPath(archiveDataPath);
    DataPath target = this.getTargetUriFunction().apply(archiveDataPath, templateMetas);
    if (!(target instanceof FsDataPath)) {
      throw new IllegalArgumentException("The target data resource (" + target + ") is not a file  resource but a " + target.getMediaType());
    }
    FsDataPath fsTarget = (FsDataPath) target;
    Path destination = fsTarget.getAbsoluteNioPath();
    String scheme = destination.toUri().getScheme();
    if (!scheme.equals("file")) {
      throw new IllegalArgumentException("The target data resource (" + target + ") is not a local file path but a " + scheme + " one");
    }
    if (Files.exists(destination)) {
      if (Files.isDirectory(destination)) {
        throw new IllegalArgumentException("The target data resource (" + target + ") calculated is a directory. Is your target data uri unique by entry? (" + this.getTargetUri() + ")");
      }
    }
    return fsTarget;
  }

  private String getEntryName(ArchiveEntry entry) {
    if (this.stripComponents == 0) {
      return entry.getName();
    }
    Path path = Paths.get(entry.getName());
    int nameCount = path.getNameCount();
    if (nameCount - 1 < this.stripComponents) {
      throw new IllegalArgumentException("We were unable to strip. The argument (" + UnZipPipelineStepArgument.STRIP_COMPONENTS.toKeyNormalizer().toCliLongOptionName() + " has a value of " + this.stripComponents + " but the entry path (" + entry.getName() + ") has only " + nameCount + " names");
    }
    return path.subpath(this.stripComponents, nameCount).toString();

  }

  public ArchiveIterator getIterator(ArchiveDataPath archiveDataPath) {
    return ArchiveIterator.builder()
      .setArchive(archiveDataPath.getArchive())
      .setNameSelector(this.entrySelector)
      .setSkipDirectory(true)
      .build();
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    List<Class<? extends ArgumentEnum>> enums = new ArrayList<>(super.getArgumentEnums());
    enums.add(UnZipPipelineStepArgument.class);
    return enums;
  }

  public DataPath getResultDataPath(ArchiveDataPath archiveDataPath, ArchiveEntry entry) {
    DataPath results;
    String archiveResultsName = "results-unzip-" + archiveDataPath.getLogicalName();
    if (entry == null) {
      results = getTabular().getMemoryConnection().getDataPath(archiveResultsName);
    } else {
      results = getTabular().getMemoryConnection().getDataPath(archiveResultsName + "-" + Fs.getFileNameWithoutExtension(Paths.get(entry.getName())));
    }
    RelationDef emptyRelationDef = results.createEmptyRelationDef();
    emptyRelationDef.addColumn("target_" + DataPathAttribute.DATA_URI.toKeyNormalizer().toSqlCase(), String.class);
    for (ArchiveEntryAttribute archiveEntryAttribute : ArchiveEntryAttribute.values()) {
      emptyRelationDef.addColumn("entry_" + archiveEntryAttribute.toKeyNormalizer().toSqlCase(), archiveEntryAttribute.getClazz());
    }
    return results;
  }

  public List<Object> getResultsRecord(ArchiveEntry entry, FsDataPath destinationDataPath) {
    List<Object> record = new ArrayList<>();
    record.add(destinationDataPath.toDataUri().toString());
    for (ArchiveEntryAttribute archiveEntryAttribute : ArchiveEntryAttribute.values()) {
      Class<?> valueClazz = archiveEntryAttribute.getClazz();
      Object valueFromAttribute = entry.getValueFromAttribute(archiveEntryAttribute);
      Object value = Casts.castSafe(valueFromAttribute, valueClazz);
      record.add(value);
    }
    return record;
  }

  public UnZipPipelineStep setStreamType(StreamType streamType) {
    this.streamType = streamType;
    return this;
  }

  public UnZipPipelineStep setStripComponents(int i) {
    this.stripComponents = i;
    return this;
  }


  @Override
  public UnZipPipelineStep setArgument(KeyNormalizer key, Object value) {

    UnZipPipelineStepArgument unZipPipelineStepArgument;
    try {
      unZipPipelineStepArgument = Casts.cast(key, UnZipPipelineStepArgument.class);
    } catch (CastException e) {
      super.setArgument(key, value);
      return this;
    }
    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(unZipPipelineStepArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + unZipPipelineStepArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (unZipPipelineStepArgument) {
      case ENTRY_SELECTOR:
        this.setEntrySelector(Glob.createOf(value.toString()));
        break;
      case STRIP_COMPONENTS:
        this.setStripComponents((Integer) attribute.getValueOrDefault());
        break;
      case OUTPUT_TYPE:
        this.setOutput((StepOutputArgument) attribute.getValueOrDefault());
        break;
      case STREAM_TYPE:
        this.setStreamType((StreamType) attribute.getValueOrDefault());
        break;
      case TARGET_DATA_URI:
        /**
         * We have taken over to set the default
         */
        super.setArgument(key, value);
        break;
      default:
        throw new InternalException("The attribute (" + key + ") of the step (" + this + ") is not in the switch branch");
    }
    return this;

  }

}
