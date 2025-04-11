# Scope

## Global

Don't use global scope. Use static function to encapsulate variable reuse instead.

Example: Move options don't need to be define at the global scope. 
You just need to encapsulate them in a function to reuse them in a DbCli command.
 