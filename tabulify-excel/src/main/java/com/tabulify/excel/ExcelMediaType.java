package com.tabulify.excel;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

public enum ExcelMediaType implements MediaType {

  XLS,
  XLSX,
  ;

  @Override
  public String getSubType() {
    switch (this) {
      case XLSX:
        return "vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      case XLS:
        return "vnd.ms-excel";
      default:
        throw new InternalException("The excel media type (" + this + ") was not processed");
    }
  }

  @Override
  public String getType() {
    return "application";
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public String getExtension() {
    return this.name().toLowerCase();
  }

  static public ExcelMediaType castFromPath(Path path) {

    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("The path (" + path + ") is a directory, not an excel file");
    }
    String extension = Fs.getExtension(path);
    try {
      return Casts.cast(extension, ExcelMediaType.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The extension (" + extension + ") is not a valid excel type. We were expecting one of :" + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ExcelMediaType.class), e);
    }

  }

  @Override
  public String toString() {
    return getType() + "/" + getSubType();
  }

}
