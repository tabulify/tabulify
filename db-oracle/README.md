# Oracle



## Test Container

We are using this docker image [wnameless/docker-oracle-xe-11g](https://github.com/wnameless/docker-oracle-xe-11g)
because the official below is too big.

```dos
docker run ^
    --name=oracle ^
    -d ^
    -p 1521:1521 ^
    -e ORACLE_ALLOW_REMOTE=true ^
    wnameless/oracle-xe-11g-r2
```

### Oracle

Not used to long, too big image.

The below text was let just for information.

[Link](https://container-registry.oracle.com/pls/apex/f?p=113:4:115223795065689::NO:4:P4_REPOSITORY,AI_REPOSITORY,AI_REPOSITORY_NAME,P4_REPOSITORY_NAME,P4_EULA_ID,P4_BUSINESS_AREA_ID:8,8,Oracle%20Database%20Standard%20Edition%202,Oracle%20Database%20Standard%20Edition%202,1,0&cs=3xO6d1T_Pz-DEcttQ99PWMOoAzQaut1tPTXCnqu952i0xk9xjncfrSrnJAfWtUSZYOqFLKA2vJMvlUqjHJwxDPA)

Steps:

  * Login
```dos
docker login container-registry.oracle.com
```
```txt
Username: nico@gerardnico.com
Password:
Login Succeeded
```

  * Run
```dos
docker run ^
    --name oracle ^
    -d ^
    -p 1521:1521 ^
    -p 5500:5500 ^
    -it ^
    container-registry.oracle.com/database/standard
```
