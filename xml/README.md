# bytle-xml
An xml command line client



## Library

  * https://github.com/jeffbr13/xq - Apply XPath expressions to XML, like jq does for JSONPath and JSON.


## Doc

  * https://rmoff.net/2020/10/01/ingesting-xml-data-into-kafka-option-3-kafka-connect-filepulse-connector/

## Example of XML / Hierarchical Parsing

Query xml with Sql: https://cdn.cdata.com/help/DWE/jdbc/pg_RESTParsing.htm

The driver offers three basic configurations to model nested data in XML and JSON as tables.

See the following sections for examples of parsing objects and arrays.
  * Flattened Documents Model: Implicitly join nested object arrays into a single table.
  * Relational Model: Model object arrays as individual tables containing a primary key and a foreign key that links to the parent document.
  * Top-Level Document Model: Model a top-level view of a document. Nested object arrays are returned as aggregates.

Example are shown with this [raw data](https://cdn.cdata.com/help/DWE/jdbc/pg_rawxml.htm)

## Transformation

  * From SQL to XML - SqlServer : https://stackoverflow.com/questions/36767649/how-to-convert-table-data-into-xml-format-using-sql-with-multiple-sub-nodes
  * XSLT - https://stackoverflow.com/questions/8623540/how-to-display-this-xml-data-in-html-tabular-format
