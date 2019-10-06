REM STARTER_CLASSPATH
REM !!!!!!!!! The GROOVY_HOME\bin\startGroovy must be modified with the below line to accept an extra path in the STARTER_CLASSPATH
REM
REM set STARTER_CLASSPATH=%STARTER_CLASSPATH%;%GROOVY_HOME%\lib\groovy-2.4.7.jar
REM
REM because the FileSystemProvider SPI of Java use the System class loader and not the Groovy thread loader
REM therefore the classPath parameters must include all File System providers
REM
set STARTER_CLASSPATH=%M2_HOME%\repository\net\bytle\bytle-sftp\1.0.0\bytle-sftp-1.0.0.jar;%M2_HOME%\repository\com\jcraft\jsch\0.1.51\jsch-0.1.51.jar

REM Run the script
groovy traversing_method_2.groovy
