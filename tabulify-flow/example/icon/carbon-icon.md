# Carbon Icon Extraction

## About

Carbon extraction of the icon from the [reference](https://carbon-elements.netlify.app/icons/examples/preview/)

The goal is to pick only the [32 in size](https://github.com/carbon-design-system/carbon/tree/main/packages/icons/src/svg/32)


## Steps

  * Get the html table from here: https://carbon-elements.netlify.app/icons/examples/preview/
  * Convert it [here](https://www.convertcsv.com/html-table-to-csv.htm)
  * Load it
```bash
tabli data copy carbon.csv@desktop carbon@sqlite
```
  * Processing
```sql
create table carbon_dictionary as select lower(name) as name, name as file_system_name  from carbon where size = '32x32' order by name;
update carbon_dictionary set name = REPLACE (name, "--","-");
```
  * Download it
```bash
tabul data copy carbon_dictionary@sqlite carbon-dictionary.csv@desktop
```
  * Json: Notepad replace (find->replace)
```regexp
([^,]*),(.*)\n
```
```
"\1":"\2",\n
```

