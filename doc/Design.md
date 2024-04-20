# Design

Goals:
  * Number 1: Maintainability: easy to maintain,
    * no third party techno (1 DB),
    * no duplicate code,
    * composition (not configuration)
  * Number 2: Usability (no bad position (in the middle, you need translate-middle), not clean and uniform design)
  * Number 3: Delete is good, see rule 1.

## Framework

### Everything is a data resource

Variation of the [Everything is a file](https://en.wikipedia.org/wiki/Everything_is_a_file) paradigm where:

  * Everything is a `data resources`
  * Everything is a `data resources` with a descriptor `DataDef`
  * The `data path` is the addressing system and the `insert / read stream` being the byte stream I/O interface.

### Batch and streaming convergence

Reprocessing: process real-time data and re-process historical data in the same framework

## Code Gold rule / Law - Constitution of design

  * Create objects as soon as possible, ie after the main. ie pass objects, not primary data type to a function. Why? Because
      * they control the structure at creation, and we don't need to deal with this error at a latter stage.
      * we will not make ew error if we need to pass an object type versus a string
      * they allow for normalization of output (ie email should be lowercase without alias)
      * they help with signature controle (if the type does not match versus string, string)
    * Example:
      * email address as string vs email as object
      * domain name vs string
  * Due to the first rule (use object as much as possible for data), pojo can be used internally as parameter because if the serialization and deserialization of type is implemented
  * Don't return `Null`. Throw instead to avoid a NPE. NULL is data type. It means that it does not exist or was not set and is a database type not a functional type.
  * Explicit is better than implicit
* Builder pattern:
  * most object building needs some cross validation step (validation between argument)
  * helps build an object as soon as possible in the process.
* No bad experience (ie no 404 or internal error) - fix them automatically or shows a sorry/meaningful message with a
  resolution
* Scope - No Global Cache for variable (in memory) - Wel Persistence
  * Cache can be a local ephemeral cache in a function (example a function that build in one call several tables for instance)
  * No default value on mandatory options or argument - ie target database should be seen in the Cli Command
  * Every object should be created with a name.
  * No side modification. No rules that changes the value of a parameter because of another parameter value, the parameter is chosen (ie no implicit, composition instead)
  * Don't put the functionality in the data - Example - LowerCase Key search should be in a specific get function not on the set method
  * Use words (enum if possible), not numbers to specify an object. The code should be readable. Bad Example: 0 -> object, 1 -> schema, 2 -> catalog
  * No primitive, no array. Box data type and List (why ? because, they bring by default extra). Varargs are still permitted as they brought a better reading of the language.
  * Parent relationship (one to many to build a dag), no many-to-many relationship (Example: a data column generator should not be able to produce data for two columns)
  * CQRS (no update in a get) - why ? because recursion is the gold standard and if you pass two times (two gets), you would get two transformations (and that's never what you want). Ex: get a property for a data resource.
  * The SQL name are in lowercase because you don't need to press the shift key (less RSI) and that uppercase means shouting from [Netiquette Guidelines - Use mixed case.  UPPER CASE LOOKS AS IF YOU'RE SHOUTING.](https://www.ietf.org/rfc/rfc1855.txt)
  * Long naming is better than short (Example: `TransferLocalWithQueryAsSource`). It gives more sense and is more easy to select when navigating.
  * No Minus in name because sql interpret it as `minus` ie tpcds-query is seen as tpcds minus query
  * Argument: Pass object, not primary type (passing an object with a `toString` is a red flag)
  * Declarative (Pass Object), not procedural (Don't pass variable)
  * Refactor early (as soon as possible) - waiting will increase the impact because the code base and test will grow
  * Throw an exception when you can give any context to the end user over what it should change
  * Throw an exception at object build-time, not at getting time
  * Functional Logic should be located where it happens.
    * For instance, if you receive a blank name from a csv (meaning null), you don't want to update it.
    * You should add this function close to the update, not in the set function. Otherwise, it's difficult to read why there is a condition.


## Functional

### Object Creator

An object creator returns an object if it does not exist on disk

There is two static function for that:

  * A `of` or `createOf` function is a scalar function and never return null. It takes a name as argument
  * A `getOrCreate` function returns a list of objects that may be empty. It accepts as argument an array of a glob pattern

### Object Id

Every object has a name as identifier in its own namespace (not the fully qualified).

### Setter / Getter

  * A get function may return null if a variable was never set
  * A set function handle always null and never return an exception for a null value (to be able to chain)
  * The default values are returned in the get function and are never set on the object property to be able to know if the value was set or not.

### Processing function

  * `create` function are processing function

### Data Processing

  * Meta above data. Example: Insert statement modified in place of switching the columns


## Dictionary

  * Data Resources have `attributes`
     * not `columns` (`columns` is a sql term and `Tabulify` try to unify structures)
     * not `property`
     * because this is the original term for a `relation` and it's also an analytical term found in `data modeling` and `OLAP`
  * Data Resources have `records`
     * not `row` (`row` is a sql term and `Tabulify` try to unify structures)
     * nor `tuple` (not popular name with SQL and analytics)
  * Data Resources have `fields` which are at the intersection of `records` and `attributes`
