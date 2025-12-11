# Combostrap Cli - A SDK for creating Command Line (Cli) utility

## About

Combostrap Cli is an SDK that helps you to create command line utility on Java.

## Status

Deprecated for [Picocli](https://picocli.info/)

## Example

```bash
cli_name command -option <arg>
```

Categorization of the passed arguments as several word type:

* command. A command is sub command line utility inside the main command line.
* option
* an option that does not expect a value is known as a flag
* an option that expects a value is known as a property
* or argument (values generally at the end, known also as operand)

## Concept

In the Combostrap Cli library:

* A command is called a [CliCommand](src/main/java/com/tabulify/cli/CliCommand.java)
* A CliCommand can have children [CliCommand](src/main/java/com/tabulify/cli/CliCommand.java)
* The cli is the root CliCommand on the tree (the first one created)
* A CliCommand expects a list of [word](src/main/java/com/tabulify/cli/CliWord.java) that are categorized as:
  * command
  * flag
  * property
  * argument

## Value Order of Precedence

* 1 - first command line
* 2 - then system property (normally only the config file location)
* 3 - then environment variable
* 4 - then config property (yaml)
* 5 - then default value

Same as in [hadoop](https://hadoop.apache.org/docs/stable/api/org/apache/hadoop/conf/Configuration.html)

## Parsing

There is only two parsing mode:

* a module
* an end command

### Module Parsing

A module parsing is a parsing that occurs against a command that is not a leaf
command in the chain of command.

A module has no arguments.

This parsing will scan the words to find:

* the command words.
* and the options (the options of the first module are generally global options valid for all commands)

Because the value of property may be taken for command, the command are expected to be before
the first option. After the first option, if a command word is found, it's discarded.

### End command parsing

In this parsing, the command is the end command and does not expect any
child command.

Every words should be known otherwise an error is reported.

## Usage / Synopsis

* Utility names should be lowercase letters between two and nine characters, inclusive.
* Names of parameters that require substitution by actual values are shown with embedded <underscore> characters.
* The angle brackets `<>` are used for the symbolic grouping of a phrase representing a single parameter
* Arguments or option-arguments enclosed in the '[' and ']' notation are optional
* Options are usually listed in alphabetical order
* The -f option is required to appear at least once and may appear multiple times.

```bash
utility_name -f option_argument [-f option_argument]... [operand...]
```

## Features

* Hierarchy of command - Several sub-command are possible
* Usage function that give a standard usage output
* Input Parsing after definition of the expected word
* Data type transformation (Double, Content of a file,...) when getting the string value of a word
* Global definition of word that can be used by different sub-command
* Options may be passed (in order of precedence) via:
  * a config properties file
  * an environment variable
  * the arguments of the main
* Mandatory option and argument at the command level
* Use of config ini file to pass global and local configuration
* Options value can be passed through config file, environment variable, Java System property, default value and args
  command line
* All option accepts multiple values

## Request

* Mapfields support (similar to Java’s system properties -Dkey=value ??) - Example: `-uDAYS=3 -u HOURS=23 -u=MINUTES=59`
* two dashes `--` separation character between option and argument
* flag
  management [Posix clustered short option](http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html#tag_12_02)

```
-abcfInputFile.txt
-abcf=InputFile.txt
-abc -f=InputFile.txt
-ab -cf=InputFile.txt
-a -b -c -fInputFile.txt
-a -b -c -f InputFile.txt
-a -b -c -f=InputFile.txt
```

* Arguments separated by the '|' ( <vertical-line>) bar notation are mutually-exclusive.
* Ellipses ( "..." ) are used to denote that one or more occurrences of an operand are allowed.

```bash
cli_name [-g option_argument]...[operand...]
```

* Flags may be grouped together
* The first -- argument that is not an option-argument should be accepted as a delimiter indicating the end of options.

## Annexes

### Others command lines library

* https://picocli.info/
* https://commons.apache.org/proper/commons-cli/
* https://github.com/kohsuke/args4j
* https://github.com/airlift/airline
