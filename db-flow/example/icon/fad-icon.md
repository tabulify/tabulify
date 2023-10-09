# Fad Icon Processing


## Steps

  * Get the data at https://github.com/fefanto/fontaudio/tree/master/svgs
  * Copy it into notepad
  * Load it as csv
```sqlite-sql
create table fad_dictionary as select replace(SUBSTRING(lower(name),5),".svg","") as name, replace(name,".svg","") as physical_name from fad where name like '%.svg'
```
  * Download it
```bash
tabli data copy fad_dictionary@sqlite fad-dictionary.csv@desktop
```
