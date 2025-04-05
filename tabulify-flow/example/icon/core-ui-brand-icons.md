# Core Ui


## Brands

  * With my hand: https://github.com/coreui/coreui-icons/tree/master/svg/brand
  * Load the csv
```
tabli data copy cib-data.csv@desktop cib@sqlite
```
  * Procesing
```sql
delete from cib where name not like 'cib%';
create table cib_dict as select replace(replace(name,"cib-",""),".svg","") as name, replace(name,".svg","") as physical_name from cib;
```
  * Download
```bash
tabli data copy cib_dict@sqlite cib-dict.csv@desktop
```
