# Icomoon Dictionary


## Steps
  * Go at https://github.com/Keyamoon/IcoMoon-Free/tree/master/SVG
  * Copy the data
  * Upload it
```bash
tabli data copy -a "headerRowId=0" icomoon.csv@desktop icomoon@sqlite
```
  * Process it
```sql
delete from icomoon where header not like '%.svg';
select * from icomoon ;
create table icomoon_dic as select replace(substr(header,5),".svg","") as name, replace(header,".svg","") as phsyical_name from icomoon ;
```
  * Download it
```bash
tabli data copy icomoon_dic@sqlite icomoon-dict.csv@desktop
```
