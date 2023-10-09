package net.bytle.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CliWordGroup {


  private static Map<String, CliWordGroup> groups = new HashMap<>();
  private final String name;
  private final CliCommand command;

  /**
   * In which level the word of this group will be shown
   * in the help
   * 1 - is more important than 2
   * <p>
   * A group on level 1 will always be present in the help
   * A group on level 2 will be present in the big HELP
   */
  private int importanceLevel = 1;

  public CliWordGroup(CliCommand cliCommand, String name) {

    this.command = cliCommand;
    this.name = name;

  }

  public static CliWordGroup get(CliCommand cliCommand, String name) {

    // Unique Name
    String uniqueName = cliCommand.getName() + name;
    CliWordGroup cliWordGroup = groups.get(uniqueName);
    if (cliWordGroup == null) {
      cliWordGroup = new CliWordGroup(cliCommand, name);
      groups.put(uniqueName, cliWordGroup);
    }
    return cliWordGroup;

  }

  public CliWordGroup setImportanceLevel(int i) {
    this.importanceLevel = i;
    return this;
  }

  public int getImportanceLevel() {
    return importanceLevel;
  }

  public String getName() {
    return this.name;
  }

  public CliWordGroup addWordOf(String name) {
    this.command
      .getOrCreateWordOf(name)
      .setGroup(this);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CliWordGroup that = (CliWordGroup) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
