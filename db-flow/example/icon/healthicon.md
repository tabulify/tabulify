# Health Icon



## Processing

Load the json file [HealthIcon](https://raw.githubusercontent.com/resolvetosavelives/healthicons/main/public/icons/meta-data.json).
```bash
tabli data copy .\healthicons-meta-data.json healthicon@sqlite
```

Extract the data
```sqlite
create table healthicon_dict as
select replace(id,"_","-") as name, path as physical_name
  from(
    SELECT json_extract(json_each.value, '$.id') as id, json_extract(json_each.value, '$.path') as path
    FROM healthicon, json_each(healthicon.json)
  )
  order by id asc;
```

* Download it
```bash
tabli data copy healthicon_dict@sqlite healthicon_dict.csv@desktop
```

* Transform it as Json with Notepad
*
* There were duplicate id for instance `pharmacy`, `heart` that we have deleted by hand
