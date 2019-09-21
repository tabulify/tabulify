# Design

## Gold rule

  * No Cache (in memory) - Wel Persistence
  * No default value on mandatory options or argument - ie target database should be seen in the Cli Command
  * Every object should be created with a name
  * Explicit (no implicit)
  * Don't put the functionality in the data - Example - LowerCase Key search should be in a specific get function not on the set method
  
## Functional

### Object Creator

An object creator returns an object if it does not exist on disk

There is two static function for that:

  * A `of` function is a scalar function and never return null. It takes a name as argument 
  * A `get` function returns a list of objects that may be empty. It accepts as argument an array of a glob pattern

### Object Id

Every object has a name as identifier in its own namespace (not the fully qualified).
 
### Setter / Getter
  
  * A get function may return null if a variable was never set
  * A set function handle always null and never return an exception for a null value (to be able to chain)
  * The default values are returned in the get function and are never set on the object property to be able to know if the value was set or not.

### Data Processing

  * Meta above data. Example: Insert statement modified in place of switching the columns   
  
