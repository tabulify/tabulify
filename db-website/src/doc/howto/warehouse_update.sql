select
  w_warehouse_sk,
  w_warehouse_sq_ft+10 as w_warehouse_sq_ft, -- the name of the column match the name of the target column
  w_warehouse_sq_ft as w_warehouse_sq_ft_old_value -- just to show the diff
from
  warehouse w2
;
