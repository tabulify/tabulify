:: This script will install the jar file that are not available in any public repository
:: On your local repository

:: On a remote server, go to the project root

:: cd ...
mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo-private -Dfile="C:\sapHdbclient\ngdbc.jar" -DgroupId=com.sap -DartifactId=ngdbc -Dversion=1.0 -DgeneratePom=true -Durl=%bytle_m2_repo_private_url%
mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo-private -Dfile="C:\sapHdbclient\ngdbc.jar" -DgroupId=com.sap -DartifactId=ngdbc -Dversion=1.0 -DgeneratePom=true -Durl=scp://sftp.bytle.net:22/path/to/repo
mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo -Dfile="C:\Users\gerard\Desktop\jre-1.7.80-windows-x64.zip" -DgroupId=com.oracle -DartifactId=jre -Dversion=1.7.80 -Dpackaging=zip -Dclassifier=windows-x64 -Durl=scp://sftp.bytle.net:22/home/gerardni-m2repo
mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo-private -Dfile="c:\tmp\ojdbc-6.jar" -DgroupId=com.oracle -DartifactId=ojdbc  -Dversion=6 -DgeneratePom=true -Durl=%bytle_m2_repo_private_url%


mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo -Dfile="C:\Users\gerard\Desktop\jre-1.8.144-windows-x64.zip" -DgroupId=com.oracle -DartifactId=jre -Dversion=1.8.144 -Dpackaging=zip -Dclassifier=windows-x64 -Durl=scp://sftp.bytle.net:22/home/gerardni-m2repo
mvn deploy:deploy-file -DrepositoryId=bytle-m2-repo -Dfile="C:\Users\gerard\Desktop\jre-1.8.144-linux-x64.zip" -DgroupId=com.oracle -DartifactId=jre -Dversion=1.8.144 -Dpackaging=zip -Dclassifier=linux-x64 -Durl=scp://sftp.bytle.net:22/home/gerardni-m2repo
:: In the local repository

:: Oracle Driver
:: From a client installation
cd /D %ORACLE_HOME%/jdbc/lib
mvn install:install-file -Dfile=ojdbc6.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.2.0 -Dpackaging=jar -DgeneratePom=true

:: Microsoft Driver
:: https://www.microsoft.com/en-us/download/details.aspx?id=11774
cd /D %DOWNLOAD_LOCATION%
mvn install:install-file -Dfile=sqljdbc41.jar -DgroupId=com.microsoft -DartifactId=sqljdbc -Dversion=4.1 -Dpackaging=jar

:: Sap Hana
cd /D %SAP_HDB_HOME%
mvn install:install-file -Dfile=ngdbc.jar -DgroupId=com.sap -DartifactId=jdbc -Dversion=1 -Dpackaging=jar

:: Oracle Timesten
:: After installation, on C:\TimesTen\tt1122_64
cd /D %TT_HOME%\lib
mvn install:install-file -Dfile=ttjdbc6.jar -DgroupId=com.oracle -DartifactId=ttjdbc -Dversion=6 -Dpackaging=jar

:: Oracle OBIEE (from Client Tool Installation)
cd %BI_HOME%\oraclebi\orahome\bifoundation\jdbc\jdk16
mvn install:install-file -Dfile=bijdbc.jar -DgroupId=com.oracle -DartifactId=bijdbc -Dversion=1 -Dpackaging=jar
