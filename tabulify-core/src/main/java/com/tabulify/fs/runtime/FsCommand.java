package com.tabulify.fs.runtime;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsDataPathAbs;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.spi.*;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.template.TemplateMetas;
import com.tabulify.template.TemplatePrefix;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.MissingSwitchBranch;
import net.bytle.exec.OsExec;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.time.DurationShort;
import org.zeroturnaround.exec.ProcessResult;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tabulify.fs.runtime.FsCommandAttribute.STDERR_DATA_URI;
import static com.tabulify.fs.runtime.FsCommandAttribute.STDOUT_DATA_URI;

/**
 * Represents the runtime of an operating system executable
 */
public class FsCommand extends FsDataPathAbs {


  public static final KeyNormalizer EXECUTION_PREFIX = KeyNormalizer.createSafe("execution");
  public static final KeyNormalizer START_TIME = KeyNormalizer.createSafe("start_time");
  List<String> arguments;


  public Integer stdBufferSize;

  public boolean executableOnly;
  Path workingDirectory;
  Duration timeoutDuration;

  private DataUriStringNode stdOutDataUriString = (DataUriStringNode) STDOUT_DATA_URI.getDefaultValue();

  private DataUriStringNode stdErrDataUriString = (DataUriStringNode) FsCommandAttribute.STDERR_DATA_URI.getDefaultValue();
  private DataUriStringNode resultsDataUri;

  /**
   * The result {@link #execute()} as run if it's not null
   */
  private FsDataPath resultPath;


  public FsCommand(FsConnection fsConnection, DataPath executableDataPath) {
    super(fsConnection, executableDataPath);
  }


  public FsCommand setStdErrDataUriString(DataUriStringNode dataUriString) {
    this.stdErrDataUriString = dataUriString;
    return this;
  }

  public FsCommand setStdOutDataUriString(DataUriStringNode dataUriString) {
    this.stdOutDataUriString = dataUriString;
    return this;
  }

  public boolean getExecutableOnly() {
    return this.executableOnly;
  }

  public List<String> getArguments() {
    return this.arguments;
  }

  public Path getWorkingDirectory() {
    return this.workingDirectory;
  }

  public Duration getTimeout() {
    return this.timeoutDuration;
  }


  public Integer getOutputBufferSize() {
    return this.stdBufferSize;
  }


  @Override
  public FsFileManager getFileManager() {
    return null;
  }

  @Override
  public Long getCount() {
    return 0L;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    return null;
  }

  @Override
  public SelectStream getSelectStream() throws SelectException {
    return execute().getSelectStream();
  }

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public SchemaType getSchemaType() {
    return null;
  }


  /**
   * See also how GitHub handles shell
   * <a href="https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions#jobsjob_idstepsrun">...</a>
   * <p>
   * <a href="https://www.nextflow.io/docs/latest/process.html">...</a>
   * The shebang declaration for a Perl script, for example, would look like: #!/usr/bin/env perl
   */
  @Override
  public FsDataPath execute() {

    /**
     * Execute may be called from different place
     * (For instance, to get the path in info)
     * We make sure we call it only once
     */
    if (this.resultPath != null) {
      return resultPath;
    }

    try {
      /**
       * The stdout
       */
      Tabular tabular = this.getConnection().getTabular();
      DataUriNode stdoutDataUri = tabular.createDataUri(this.stdOutDataUriString);
      TemplateUriFunction stdOutDataUriFunction = TemplateUriFunction.builder(tabular)
        .setTargetUri(stdoutDataUri)
        .setExtraTemplatePrefixes(Set.of(EXECUTION_PREFIX))
        .setStrict(tabular.isStrictExecution())
        .build();

      /**
       * The stderr
       */
      DataUriNode stderrDataUri = tabular.createDataUri(this.stdErrDataUriString);
      TemplateUriFunction stdErrDataUriFunction = TemplateUriFunction.builder(tabular)
        .setTargetUri(stderrDataUri)
        .setExtraTemplatePrefixes(Set.of(EXECUTION_PREFIX))
        .setStrict(tabular.isStrictExecution())
        .build();

      DataPath executableDataPath = this.getExecutableDataPath();

      if (!(executableDataPath instanceof FsDataPath)) {
        throw new IllegalArgumentException("The executable data path is not a file but a " + executableDataPath.getMediaType() + ". It's execution as command is not yet supported");
      }
      FsDataPath fsExecutableDataPath = (FsDataPath) executableDataPath;

      /**
       * Builder
       */

      OsExec.OsExecBuilder osExecBuilder = OsExec.builder()
        .setArguments(getArguments())
        .setExecutableOnly(getExecutableOnly())
        .setExecutablePath(fsExecutableDataPath.getAbsoluteNioPath());

      /**
       * Stdout (Mandatory)
       */
      MetaMap metaMap = new MetaMap(tabular);
      metaMap.put(START_TIME, Timestamp.from(Instant.now()));
      TemplateMetas templateMetas = TemplateMetas
        .builder()
        .addMeta(metaMap, EXECUTION_PREFIX)
        .addDataPath(fsExecutableDataPath, TemplatePrefix.EXECUTABLE);
      DataPath stdOutDataPath = stdOutDataUriFunction.apply(fsExecutableDataPath, templateMetas);
      if (!(stdOutDataPath instanceof FsDataPath)) {
        throw new IllegalArgumentException("The stdout data path (" + stdOutDataPath + ") is not a file but a " + executableDataPath.getMediaType() + ". Change the " + STDOUT_DATA_URI + " (" + this.stdOutDataUriString + ") to a file system connection");
      }
      FsDataPath stdOutFsDataPath = (FsDataPath) stdOutDataPath;
      osExecBuilder.setStandardOutputPath(stdOutFsDataPath.getAbsoluteNioPath());
      /**
       * Stderr (Mandatory)
       */
      DataPath stdErrDataPath = stdErrDataUriFunction.apply(fsExecutableDataPath, templateMetas);
      if (!(stdErrDataPath instanceof FsDataPath)) {
        throw new IllegalArgumentException("The stderr data path (" + stdErrDataPath + ") is not a file but a " + stdErrDataPath.getMediaType() + ". Change the " + STDERR_DATA_URI + " (" + this.stdErrDataUriString + ") to a file system connection");
      }

      FsDataPath fsStdErrDataPath = (FsDataPath) stdErrDataPath;
      osExecBuilder.setStandardErrorPath(fsStdErrDataPath.getAbsoluteNioPath());

      /**
       * Working Directory
       */
      Path workingDirectory = getWorkingDirectory();
      if (workingDirectory == null) {
        // ie tmp in (shell.sh@cd)@tmp
        // the shell script present in the current directory is executing a tmp
        workingDirectory = this.getConnection().getCurrentDataPath().getAbsoluteNioPath();
      }

      if (!Files.isDirectory(workingDirectory)) {
        throw new IllegalArgumentException("The working directory specified (" + workingDirectory + ") is not a directory");
      }
      osExecBuilder.setWorkingDirectory(workingDirectory.toAbsolutePath());

      /**
       * Timeout
       */
      Duration timeout = getTimeout();
      if (timeout != null) {
        osExecBuilder.setTimeout(timeout.toSeconds(), TimeUnit.SECONDS);
      }

      /**
       * Character set
       * Could be a binary executable
       */
      if (fsExecutableDataPath instanceof FsTextDataPath) {
        Charset characterSet = ((FsTextDataPath) fsExecutableDataPath).getCharset();
        osExecBuilder.setCharset(characterSet);
      }

      /**
       * Buffer size
       */
      Integer bufferSize = this.getOutputBufferSize();
      if (bufferSize != null) {
        osExecBuilder.setStandardStreamBufferSize(bufferSize);
      }


      /**
       * Execution
       */
      ProcessResult result = osExecBuilder.build().execute();
      if (result.getExitValue() != 0) {
        String message = "An error occurred while executing the command (" + this + ")";
        if (Tabulars.exists(stdErrDataPath)) {
          try {
            message += "\nErrors from the error log (" + stdErrDataPath + "):\n" + Fs.getFileContent(fsStdErrDataPath.getAbsoluteNioPath());
          } catch (NoSuchFileException e) {
            throw new InternalException("should not happen as we check for file existence", e);
          }
        }
        throw new RuntimeException(message);
      }

      /**
       * Target
       */
      if (resultsDataUri != null) {
        TemplateUriFunction targetUriFunction = TemplateUriFunction.builder(tabular)
          .setTargetUri(tabular.createDataUri(resultsDataUri))
          .setStrict(tabular.isStrictExecution())
          .setExtraTemplatePrefixes(Set.of(EXECUTION_PREFIX))
          .build();
        resultPath = (FsDataPath) targetUriFunction.apply(fsExecutableDataPath, templateMetas);
      } else {
        resultPath = stdOutFsDataPath;
      }
      return resultPath;
    } catch (Exception e) {
      throw new RuntimeException("An error occurred while executing the runtime resource (" + this + "). Error: " + e.getMessage(), e);
    }

  }

  public FsCommand setArguments(List<String> arguments) {
    this.arguments = arguments;
    return this;
  }

  @Override
  public DataPath addAttribute(KeyNormalizer key, Object value) {
    FsCommandAttribute commandAttribute;
    try {
      commandAttribute = Casts.cast(key, FsCommandAttribute.class);
    } catch (CastException e) {
      // may be a common attribute (logical name)
      return super.addAttribute(key, value);
    }
    Attribute attribute;
    try {
      attribute = this.getConnection().getTabular().getVault()
        .createVariableBuilderFromAttribute(commandAttribute)
        .setOrigin(Origin.MANIFEST)
        .build(value);
      this.addAttribute(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + commandAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (commandAttribute) {
      case ARGUMENTS:
        List<String> arguments;
        try {
          arguments = Casts.castToNewList(attribute.getValueOrDefault(), String.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + commandAttribute + " value is not a valid list of string. Error: " + e.getMessage(), e);
        }
        return this.setArguments(arguments);
      case WORKING_DIRECTORY:
        Path workingDirectory = Paths.get((String) attribute.getValueOrDefault());
        return this.setWorkingDirectory(workingDirectory);
      case STDOUT_DATA_URI:
        DataUriStringNode dataUriStringNode = (DataUriStringNode) attribute.getValueOrDefault();
        return this.setStdOutDataUriString(dataUriStringNode);
      case STDERR_DATA_URI:
        DataUriStringNode dataUriStringNodeErr = (DataUriStringNode) attribute.getValueOrDefault();
        return this.setStdErrDataUriString(dataUriStringNodeErr);
      case TIME_OUT:
        return this.setTimeOut((DurationShort) attribute.getValueOrDefault());
      case RESULT_DATA_URI:
        return this.setStdErrDataUriString((DataUriStringNode) attribute.getValueOrDefault());
      case STD_BUFFER_SIZE:
        return this.setStdBufferSize((Integer) attribute.getValueOrDefault());
      default:
        throw new MissingSwitchBranch("commandAttribute", commandAttribute);
    }

  }

  public DataPath setStdBufferSize(Integer bufferSize) {
    this.stdBufferSize = bufferSize;
    return this;
  }

  public DataPath setTimeOut(DurationShort durationShort) {
    this.timeoutDuration = durationShort.getDuration();
    return this;
  }

  private FsCommand setWorkingDirectory(Path workingDirectory) {
    this.workingDirectory = workingDirectory;
    return this;
  }

}
