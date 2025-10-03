package net.bytle.template.flow;

import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import net.bytle.exception.InternalException;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.nio.file.Path;

/**
 * The memory representation of a template
 */
class TemplateModel {

  private MediaType type;
  private String content;
  // anonymous is inline
  private String logicalName = "inline";
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

  TemplateModel setContent(String content) {
    this.content = content;
    return this;
  }

  public TemplateModel setMediaType(MediaType type) {
    this.type = type;
    return this;
  }

  public MediaType getMediaType() {
    // Type may be null as this is the type of template if given inline (ie content)
    if (this.type == null) {
      if (this.templateOriginDataPath == null) {
        // The check should happen at input time, that's why it's internal
        throw new InternalException("We can't determine the template type as the template data path and the template type attribute are empty.");
      }
      return this.templateOriginDataPath.getMediaType();
    }
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
