# Cross Loader Test (Move/Copy - Data Manipulations actions - ELT) 

## About

The module test cross data system move/copy - loading actions (insert, update, merge)

This module has only test, the entry code is in 
  * `Tabulars.move`
  * and the package `net.bytle.db.move`

## Alias

The term `Move` is also known as:

  * Transport
  * Transfer
  * Extract / Load
  * Processing
  * Cut / Copy / Paste

`Move` was preferred because it gives the notion of atomic operations.

`Copy` was not chosen because this is one of the first task in the well-known `Cut-Copy/Paste` operation
and therefore does not describe the operation as a whole.
 
## Idempotency 

is idempotent (same request will lead to the same result):
  * Replace (delete/create) 
  * Update 
  * Merge/upsert

is not idempotent
  * Insert (append)

## Type 
### CRUD

   * create (insert/append), 
   * read (select/retrieve/get), 
   * update (modify) 
   * delete (destroy)

### ETL
Extract, transform, load (ETL) is the general procedure of copying data from one or more sources into a destination system

  * TEL (Transform, extract, load)
  * Extract, load, transform (ELT)

### Cut / Copy / Paste

* [Cut / Copy / Paste](https://en.wikipedia.org/wiki/Cut,_copy,_and_paste) - The command names are an interface metaphor based on the physical procedure used in manuscript editing to create a page layout.
    * `Copy` - The copy command creates a duplicate
    * `Cut` - The cut command removes the selected data from its original position
    * In both cases the selected data is kept in temporary storage (the clipboard)
    * The data from the clipboard is later inserted wherever a `paste` command is issued.
    * The data remains available to any application supporting the feature, thus allowing easy data transfer between applications.
    * Cut / Paste = Move
    * Copy / Paste = Copy

## Method

Short-cut alias of the `move` method that we may find in `Tabulars`:

 
  * merge - get all source data and upsert the data to the target
  * copy (a copy operations) - get all source data, verify that the target is empty and insert the data to the target
  * cut (a cut operations) - get all source data, verify that the target is empty, insert them to the target and delete the source
  * insert - get all source data and insert the data to the target
  * replace - get all source data, recreate or truncate the target, and insert the data to the target
  * move (default to a non-strict merge operations)

## Strict mode impact



^ Operations ^ Mode ^ Merge ^ Copy ^ Cut ^ Insert ^ Replace ^
| Error if the target table does not exists | Strict | Yes | - | - | Yes | Yes |
| Error if the target table does not exists | Non-Strict | No | - | - | No | No |
| Error if the target table exists | Strict | - | Yes | Yes | - | - |
| Error if the target table exists | Non-Strict | - | No | No | - | - |
| Error if the target table is not empty | Strict | - | - | - | - | - |
| Error if the target table is not empty | Non-Strict | - | Yes | Yes | - | - |


## Move Options

  * Operations on the source path
    * Drop (A file move is doing that)
    * Truncate (Delete content not file or table)
  * Operations on the target path
    * Drop
    * DropIfExist
    * Truncate (Delete content)
    * TruncateIfExist (Delete content)
    * Create
    * CreateIfNotExist
    * Copy Permissions / Attributes / Metadata
  * Operations on the target data 
    * Insert/Append - get all source data and insert/append them
    * Update - get all source data and update them
    * Merge/Upsert - get all source data and update/insert them