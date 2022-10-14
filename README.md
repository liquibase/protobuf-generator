# Liquibase Protobuf Generator
Liquibase Extension that will generate a [protobuf](https://developers.google.com/protocol-buffers/) file for Liquibase Commands.

## Requirements
* Liquibase 4.16.1 +

## Installation
```shell
lpm update
lpm add protobuf-generator
```

## Usage
```shell
liquibase generate-protobuf 
```

### Optional Parameters
```
--output-dir=PARAM   
    Directory for protobuf output
   DEFAULT: proto
   (liquibase.command.outputDir OR liquibase.command.generateProtobuf.outputDir)
   (LIQUIBASE_COMMAND_OUTPUT_DIR OR LIQUIBASE_COMMAND_GENERATE_PROTOBUF_OUTPUT_DIR)
   [deprecated: --outputDir]

--target-command=PARAM
   Individual Command to generate protobuf
   DEFAULT:
   (liquibase.command.targetCommand OR liquibase.command.generateProtobuf.targetCommand)
   (LIQUIBASE_COMMAND_TARGET_COMMAND OR LIQUIBASE_COMMAND_GENERATE_PROTOBUF_TARGET_COMMAND)
   [deprecated: --targetCommand]
```

## Example
```
$ liquibase generate-protobuf --target-command validate --output-dir proto

####################################################
##   _     _             _ _                      ##
##  | |   (_)           (_) |                     ##
##  | |    _  __ _ _   _ _| |__   __ _ ___  ___   ##
##  | |   | |/ _` | | | | | '_ \ / _` / __|/ _ \  ##
##  | |___| | (_| | |_| | | |_) | (_| \__ \  __/  ##
##  \_____/_|\__, |\__,_|_|_.__/ \__,_|___/\___|  ##
##              | |                               ##
##              |_|                               ##
##                                                ## 
##  Get documentation at docs.liquibase.com       ##
##  Get certified courses at learn.liquibase.com  ## 
##  Free schema change activity reports at        ##
##      https://hub.liquibase.com                 ##
##                                                ##
####################################################
Starting Liquibase at 14:40:05 (version 4.16.1 #4571 built at 2022-09-11 16:02+0000)
Liquibase Version: 4.16.1
Liquibase Community 4.16.1 by Liquibase
writing validate.proto
Liquibase command 'generate-protobuf' was executed successfully.

$ cat proto/validate.proto 
syntax = "proto3";

option go_package="./;proto";
option java_package = "org.liquibase.grpc.proto";
option java_multiple_files = true;
option java_outer_classname = "ValidateProto";

/* Validate the changelog for errors */
package validate;

service ValidateService {
  rpc execute(ValidateRequest) returns (Response) {}
}

message ValidateRequest {
   string changelogFile = 1; // *required* The root changelog
   string defaultCatalogName = 2; // The default catalog name to use for the database connection
   string defaultSchemaName = 3; // The default schema name to use for the database connection
   string driver = 4; // The JDBC driver class
   string driverPropertiesFile = 5; // The JDBC driver properties file
   string password = 6; // Password to use to connect to the database
   string url = 7; // *required* The JDBC database connection URL
   string username = 8; // Username to use to connect to the database
}

message Response {
  string message = 1;
}
```

## Feedback and Issues
Please submit all feedback and issues to [GitHub Issues](https://github.com/liquibase/protobuf-generator/issues) in this repository.
