-- https://www.sqlite.org/json1.html#examples_using_json_each_and_json_tree_
SELECT DISTINCT user.name
  FROM user, json_each(user.phone)
 WHERE json_each.value LIKE '704-%';
