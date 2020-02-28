# Viz



## IDEA Encoding

### File encoding
File: to store the characters in the good encoding format, the file encoding should be `UTF8`.

```
Settings → File Encoding → Project Encoding → IDE Encoding: UTF8
```
Check the file encoding for each file in the right bottom corner.  

### Console encoding
You need to start Idea in `UTF8` and not with the default encofing `windows-1521` 
  * `Help > Edit custom VM options...`, 
  * It opens the `"C:\Users\name\.IdeaXXX\config\idea.exe.vmoptions` 
  * add the following lines
```
-Dfile.encoding=UTF-8
```
  
 



