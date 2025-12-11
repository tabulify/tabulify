-- https://www.sqlite.org/json1.html#examples_using_json_each_and_json_tree_
SELECT distinct name
FROM user, json_tree(user.data, '$.phones')
WHERE value LIKE '704-%';
