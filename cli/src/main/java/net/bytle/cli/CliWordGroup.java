package net.bytle.cli;

import java.util.HashMap;
import java.util.Map;

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
    private int level = 1;

    /**
     * A description or help text for this group
     */
    private String desc;

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

    public int getLevel() {
        return level;
    }

    public CliWordGroup setLevel(int i) {
        this.level = i;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public CliWordGroup addWordOf(String name) {
        this.command.wordOf(name)
                .setGroup(this);
        return this;
    }

    public String getDescription() {
        return this.desc;
    }

    public CliWordGroup setDescription(String desc) {
        this.desc = desc;
        return this;
    }
}
