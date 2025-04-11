# Process


## Date spin (Histogram)

From [Date Spin](http://help.zuar.com.s3-website-us-east-1.amazonaws.com/connectors/base-connectors/sql/date-spine/#:~:text=A%20date%20spine%20table%20is,exists%20in%20the%20original%20data.&text=To%20show%20how%20many%20employees,the%20company%20on%20those%20dates.)

A date spine table is useful for any use case that requires rows of dates where no dates exists 
in the original data.

For example in an employee turnover use case, you might have data that looks like this:
employee_id	start_date	end_date

```txt
1	2014-01-01	2017-03-31
2	2015-06-01	NULL
3	2017-01-01	2017-05-31
```

To show how many employees were at the company on any given day you need a different data structure, one that has every possible date and everyone who was at the company on those dates.
To solve this you need a master date / calendar table.
