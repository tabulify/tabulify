# Design

## Gold rule

  * No Cache (in memory)
  * Wel Persistence
  
## Functional

### Object Creator

An object creator returns an object if it does not exist on disk

There is two static function for that:

  * A `of` function is a scalar function and never return null. It takes a name as argument 
  * A `get` function returns a list of objects that may be empty. It accepts as argument an array of a glob pattern

### Object Id

The name of an object is its identifier.
 
### Setter / Getter
  
  * A function may return null if a variable was never set
  * A set function handle always null and never return an exception for a null value
  * The default values are returned in the get function and are never set on the object property to be able to know if the value was set or not.

### Data Processing

  * Meta above data. Example: Insert statement modified in place of switching the columns   
  
