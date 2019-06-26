# PowerShell script to build the docker image
# 
# Description: Build script for building Docker images.
# 
#

# Strict mode
Set-StrictMode -Version 1


#### 
# Functions Util
#### 

# Write-Error without stacktrace
function Write-Error($message) {
    [Console]::ForegroundColor = 'red'
    [Console]::Error.WriteLine($message)
    [Console]::ResetColor()
}

# Return the script directory which is the HOME
function Get-ScriptDirectory {
    Split-Path -parent $PSCommandPath
}

####
# Usage
# Must be the first function
####
function Print-Usage() {

$cliName = 'bdev'

  Write-Host @"

Helper to Bytle Dev

Usage: $cliName module command [options]


Module:
  * jar     : operations on the bytle jar
  * help    : print the usage

Modules Details:

    - jar: Jar
        $cliName jar install : Create the Jar and install it to the local lib directory
        $cliName files unzip : Unzip the installation files

    - help: Print the help
        $cliName help        : print the usage

"@

}


# Jar Command
function Execute-Jar {

	Write-Host "The command ($command) was called on the module ($module)".
	Write-Host ""
	

	# Command parameter verification
	Switch ($command)
	{
        "install" { 
            Write-Host "Install"
            mvn --offline -DskipTests=true -P copy-jar -f $POM_PATH install
		}
		default { 
			Write-Error "The command ($command) is unknown"
			Print-Usage
			exit(1)
		} 
	}

}

# Cli
function Execute-Cli {

	Write-Host "The command ($command) was called on the module ($module)".
	Write-Host ""


	# Command parameter verification
	Switch ($command)
	{
        "copy" {
            Write-Host "Copy"
            cp D:\maven\repository\net\bytle\bytle-cli\2.0.0-SNAPSHOT\bytle-cli-2.0.0-SNAPSHOT.jar C:\Users\gerard\bin\bytle-db-1.1.0-SNAPSHOT\lib
		}
		default {
			Write-Error "The command ($command) is unknown"
			Print-Usage
			exit(1)
		}
	}

}



##############
#### MAIN ####
##############



##############################
# Run Environment variable
##############################
$RUN_TIMESTAMP=$(Get-Date -UFormat "%Y-%m-%d_%H-%M")
$RUN_DIRECTORY="$env:TMP\obi\$RUN_TIMESTAMP"
New-Item -ItemType Directory -Force -Path  $RUN_DIRECTORY | Out-Null
$ORIGINAL_DIRECTORY=$pwd
cd $RUN_DIRECTORY

##############################
# Fix Environment variable
# They are not in the function 
# because they need to be accessible for the info command
##############################

# Project Home
$PROJECT_HOME=Get-ScriptDirectory
$POM_PATH="$PROJECT_HOME\pom.xml"

if ( $args.Count -lt 1 ) {
    Write-Error("The minimal number of argument is 1") 
    Print-Usage
    exit 1
}
$service = $args[0]
$command = $args[1];


##############################
# Call to function
##############################
Switch ($service)
{
	"help" { 
        Print-Usage 
        exit(0)
        }
    "jar" {
        Execute-Jar
        }
    "appHome" {
            Execute-Cli
            }
    "doc" {
        Write-Host "Doc Test (offline)"

        Start-Process -FilePath "mvn.cmd" `
            -NoNewWindow -Wait `
            -WorkingDirectory $PROJECT_HOME `
            -ArgumentList "exec:exec", "--offline", `
            # Executable must be Jdk
        "-Dexec.executable=`"C:\Java\jdk1.8.0_171\bin\java.exe`"",   `
             "-Dexec.args=`"-classpath %classpath DocTest $PROJECT_HOME\src\doc\pages\$command`"",   `
             "--file $PROJECT_HOME\pom.xml"
    }
	default { 
		Write-Error "
The service ($service) is unknown.
		"
		Print-Usage
		exit(1)
		} 
}

##############################
# Exit
##############################
# Go back to the 
cd $ORIGINAL_DIRECTORY
Write-Host ""
Write-Host "See log, repo and other artifacts for this run at $RUN_DIRECTORY".
Write-Host "End"
exit(0)
