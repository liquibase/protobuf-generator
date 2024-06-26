package liquibase.command;

import liquibase.configuration.ConfigurationDefinition;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.util.StringUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static liquibase.Scope.getCurrentScope;

public class GenerateProtobufCommandStep extends AbstractCommandStep {

    public static final String[] COMMAND_NAME = {"generateProtobuf"};
    public static final CommandArgumentDefinition<String> OUTPUT_DIR_ARG;
    public static final CommandArgumentDefinition<String> TARGET_COMMAND;

    static {
        CommandBuilder builder = new CommandBuilder(GenerateProtobufCommandStep.COMMAND_NAME);
        OUTPUT_DIR_ARG = builder.argument("outputDir", String.class)
                .defaultValue("proto")
                .description("Directory for protobuf output").build();
        TARGET_COMMAND = builder.argument("targetCommand", String.class)
                .defaultValue("")
                .description("Individual Command to generate protobuf").build();
    }

    @Override
    public String[][] defineCommandNames() {
        return new String[][]{COMMAND_NAME};
    }

    @Override
    public void run(CommandResultsBuilder resultsBuilder) throws Exception {
        CommandScope commandScope = resultsBuilder.getCommandScope();
        String outputDir = commandScope.getArgumentValue(OUTPUT_DIR_ARG);
        String targetCommand = commandScope.getArgumentValue(TARGET_COMMAND);

        writeGlobalToFile(outputDir);

        if (StringUtil.isNotEmpty(targetCommand)) {
            CommandDefinition command = getCommand(targetCommand);
            if (command.getHidden()) {
                throw new UnexpectedLiquibaseException("Command " + targetCommand + " cannot be generated as it is defined as a 'hidden' command by Liquibase");
            }
            writeCommandToFile(command, outputDir);
        } else {
            // Generate protobuf for all commands
            for (CommandDefinition commandDefinition : getCommands()) {
                //Don't generate protobuf for generateProtobuf command step or hidden commands
                if (commandDefinition.getName() == COMMAND_NAME || commandDefinition.getHidden()) {
                    continue;
                }
                writeCommandToFile(commandDefinition, outputDir);
            }
        }
    }


    private void writeGlobalToFile(String outputDir) throws IOException {
        String fileName = "global_options.proto";
        System.out.println( "writing "  + fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "/" + fileName))) {
            writeHeaderToFile(writer, "globalOptions");
            writer.write("package liquibase;\n\n");
            writer.write("/* Liquibase Global Options */\n");
            writer.write("message GlobalOptions {\n");
            int i = 1;
            for (ConfigurationDefinition<?> def : getCommandDefinitions()) {
                if (!def.getKey().contains("psql") && !def.getKey().contains("sqlcmd") && !def.getKey().contains("sqlplus")) {
                    String dataTypeName = def.getDataType().getSimpleName();
                    String key = def.getKey().replace("liquibase.", "");
                    String argumentName = toSnakeCase(key.replace(".", "_"));
                    if (dataTypeName.equalsIgnoreCase("string")) {
                        writer.write("  optional string " + argumentName + " = " + i + ";");
                    } else if (dataTypeName.equalsIgnoreCase("boolean")) {
                        writer.write("  optional bool " + argumentName + " = " + i + ";");
                    } else if (dataTypeName.equalsIgnoreCase("Integer")) {
                        writer.write("  optional int32 " + argumentName + " = " + i + ";");
                    } else {
                        writer.write("  optional string " + argumentName + " = " + i + ";");
                    }
                    if (def.getDescription() != null) {
                        writer.write(" // " + def.getDescription().replace("\n", "") + "\n");
                    } else if(def.getDefaultValueDescription() != null) {
                        writer.write(" // " + def.getDefaultValueDescription().replace("\n", "") + "\n");
                    } else {
                        writer.write("\n");
                    }
                    //TODO format line breaks at 80 for multiline comments
                    //TODO catch deprecated int32 old_field = 6 [deprecated = true];
                    i++;
                }
            }
            writer.write("}\n\n");
        }
    }

    private void writeCommandToFile(CommandDefinition commandDefinition, String outputDir) throws IOException {
        String commandName = commandDefinition.getName()[0];
        if (commandDefinition.getName().length > 1) {
            commandName += "_" + commandDefinition.getName()[1];
        }
        // Files should be named lower_snake_case.proto
        // https://developers.google.com/protocol-buffers/docs/style#file_structure
        commandName = toSnakeCase(commandName);
        String uCommandName = StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandName));
        String fileName = commandName + ".proto";
        System.out.println( "writing "  + fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "/" + fileName))) {
            writeHeaderToFile(writer, uCommandName);
            List<CommandStep> commandStep = commandDefinition.getPipeline();
            String cname = commandStep.get(0).getClass().getName();
            if (cname.contains("com.datical")) {
                writer.write("package liquibase.pro;\n\n");
            } else  {
                writer.write("package liquibase;\n\n");
            }
            writer.write("service " + uCommandName + "Service {\n");
            if (commandDefinition.getName().length > 1) {
                writer.write("  rpc execute(" + StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandDefinition.getName()[0])) + "." + StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandDefinition.getName()[1])) + "Request) returns (Response) {}\n");
            } else {
                writer.write("  rpc execute(" + uCommandName + "Request) returns (Response) {}\n");
            }
            writer.write("}\n\n");
            String shortDesc = commandDefinition.getShortDescription() != null ? commandDefinition.getShortDescription().replace("\n", " ") : "";
            writer.write("/* " +  shortDesc + " */\n");
            if (commandDefinition.getName().length > 1) {
                writer.write("message " + StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandDefinition.getName()[0]))  + " {\n");
                writer.write("  message " + StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandDefinition.getName()[1])) + "Request {\n");
                writeArgumentsToFile(writer, commandDefinition.getArguments(), "  ");
                writer.write("  }\n");
                writer.write("}\n\n");
            } else {
                writer.write("message " + uCommandName + "Request {\n");
                writeArgumentsToFile(writer, commandDefinition.getArguments(), "");
                writer.write("}\n\n");
            }

            writer.write("message Response {\n");
            writer.write("  string message = 1;\n");
            writer.write("}\n");
        }
    }

    private void writeHeaderToFile(BufferedWriter writer, String uCommandName) throws IOException {
        writer.write("syntax = \"proto3\";\n");
        if (uCommandName != "globalOptions") {
            writer.write("import public \"global_options.proto\";\n");
        }
        writer.write("\n");
        writer.write("option go_package=\"./;proto\";\n"); //TODO get proper golang package path
        writer.write("option java_package = \"org.liquibase.grpc.proto\";\n");
        writer.write("option java_multiple_files = true;\n");
        writer.write("option java_outer_classname = \"" + uCommandName + "Proto\";\n\n");
    }

    private void writeArgumentsToFile(BufferedWriter writer, Map<String, CommandArgumentDefinition<?>> arguments, String indent) throws IOException {
        int i=1;
        for (Map.Entry<String, CommandArgumentDefinition<?>> entry : arguments.entrySet()) {
            if (entry.getValue().getHidden()) {
                continue;
            }
            String optional = entry.getValue().isRequired() ? "" : "  optional ";
            String required = entry.getValue().isRequired() ? "*required* " : "";
            String tab = entry.getValue().isRequired() ? "  " : "";
            String dataTypeName = entry.getValue().getDataType().getSimpleName();
            String argumentName = toSnakeCase(entry.getKey());
            if (dataTypeName.equalsIgnoreCase("string")) {
                writer.write(indent + optional + tab + "string " + argumentName + " = " + Integer.toString(i) + ";");
            } else if (dataTypeName.equalsIgnoreCase("boolean")) {
                writer.write(indent + optional + tab + "bool " + argumentName + " = " + Integer.toString(i) + ";");
            } else if (dataTypeName.equalsIgnoreCase("Integer")) {
                writer.write(indent + optional + tab + "int32 " + argumentName + " = " + Integer.toString(i) + ";");
            } else {
                writer.write(indent +optional + tab + "string " + argumentName + " = " + Integer.toString(i) + ";");
            }
            if (entry.getValue().getDescription() != null) {
                writer.write(" // " + required + entry.getValue().getDescription().replace("\n", " ") + "\n");
            } else {
                writer.write("\n");
            }
            i++;
        }
        writer.write(indent +"  liquibase.GlobalOptions global_options = " + i + ";\n");
    }

    // https://www.geeksforgeeks.org/convert-camel-case-string-to-snake-case-in-java/
    private String toSnakeCase(String str) {
        String result = "";
        char c = str.charAt(0);
        result = result + Character.toLowerCase(c);
        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result = result + '_';
                result = result + Character.toLowerCase(ch);
            } else if (Character.isSpaceChar(ch)) {
                result = result + '_';
            }
            else {
                result = result + ch;
            }
        }
        return result;
    }

    // https://www.geeksforgeeks.org/convert-snake-case-string-to-camel-case-in-java/
    private static String kebabToCamel(String str) {
        str = str.substring(0, 1).toLowerCase() + str.substring(1);
        StringBuilder builder = new StringBuilder(str);
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == '-') {
                builder.deleteCharAt(i);
                builder.replace(
                    i, i + 1,
                    String.valueOf(
                        Character.toUpperCase(
                            builder.charAt(i))));
            }
        }
        return builder.toString();
    }

    private CommandDefinition getCommand(String targetCommand) {
        String[] split = kebabToCamel(targetCommand).split(" ");
        final CommandFactory commandFactory = getCurrentScope().getSingleton(CommandFactory.class);
        return commandFactory.getCommandDefinition(split);
    }

    private SortedSet<ConfigurationDefinition<?>> getCommandDefinitions() {
        final LiquibaseConfiguration liquibaseConfiguration = getCurrentScope().getSingleton(LiquibaseConfiguration.class);
        return liquibaseConfiguration.getRegisteredDefinitions(false);
    }

    private SortedSet<CommandDefinition> getCommands() {
        final CommandFactory commandFactory = getCurrentScope().getSingleton(CommandFactory.class);
        return commandFactory.getCommands(false);
    }
}
