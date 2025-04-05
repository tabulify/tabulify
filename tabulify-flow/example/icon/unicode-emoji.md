# Unicode Emoji List

## About

This example lists the steps that we did to get the code point of emojis
and their respective name.

## Steps
  * Get the html table from here: https://unicode.org/emoji/charts/full-emoji-list.html
  * Convert it here https://www.convertcsv.com/html-table-to-csv.htm
  * Load it
```bash
tabli data copy -to replace emoji.txt@desktop emoji-original@sqlite
tabli data delete emoji_filtered@sqlite
```
  * Filter the lines that don't start with a number
```sqlite-sql
create table emoji_filtered as select lines from emoji ej where substr(trim(lines),0,2) in ("0","1","2","3","4","5","6","7","8","9");
```
  * HTML to CSV didn't take into account the `colspan` html attribute. We correct it
```sqlite-sql
update emoji_filtered set lines = replace(lines,",… …,",",,,,,,,,,,,,");
```
  * Download and reload it as csv to parse it
```bash
tabli data copy emoji_filtered@sqlite emoji_filtered.txt@desktop
tabli data copy --type csv -sa 'headerRowId=0' -to replace emoji_filtered.txt@desktop emoji_filtered@sqlite
```
  * Data Processing
```sqlite-sql
create table emojis_point_name as select "col15" as "name", "col2" as "code_point" from emoji_filtered;
update emojis_point_name set name = replace(name,"⊛ ","");
update emojis_point_name set name = replace(name,":","");
update emojis_point_name set name = replace(name,"  "," ");
update emojis_point_name set name = replace(name,"  "," ");
update emojis_point_name set name = trim(name);
update emojis_point_name set name = replace(name,"“","");
update emojis_point_name set name = replace(name,"”","");
update emojis_point_name set name = replace(name,"!","");
update emojis_point_name set name = replace(name,"(","");
update emojis_point_name set name = replace(name,")","");
update emojis_point_name set name = replace(name," ","-");
update emojis_point_name set name = replace(name,",","");
update emojis_point_name set name = lower(name);
update emojis_point_name set code_point = replace(code_point,"U+","");
update emojis_point_name set code_point = trim(lower(code_point));
delete from emojis_point_name where length(code_point) != 5 or length(code_point) != 4; -- not good 2744
select * from emojis_point_name;
```
  * Prepare the download data (could be a sql)
```sqlite-sql
drop table emojis_mapping;
create table emojis_mapping as select name, code_point from emojis_point_name order by name asc;
```
  * Download it
```bash
tabli data copy -to replace emojis_mapping@sqlite emojis.csv@desktop
```
  * Json download was not working: https://csvjson.com/csv2json -with transpose
