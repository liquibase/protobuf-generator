package liquibase.command;

import liquibase.Scope;
import liquibase.command.*;
import liquibase.configuration.ConfigurationDefinition;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.servicelocator.ServiceLocator;
import liquibase.util.StringUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static liquibase.Scope.getCurrentScope;

public class GenerateProtobufCommandStep extends AbstractCommandStep {

    public static final String[] COMMAND_NAME = {"generateProtobuf"};
    public static final CommandArgumentDefinition<String> OUTPUT_DIR_ARG;

    static {
        CommandBuilder builder = new CommandBuilder(GenerateProtobufCommandStep.COMMAND_NAME);
        OUTPUT_DIR_ARG = builder.argument("outputDir", String.class)
                .defaultValue("proto")
                .description("Director for protobuf output").build();
    }

    @Override
    public String[][] defineCommandNames() {
        return new String[][]{COMMAND_NAME};
    }

    @Override
    public void run(CommandResultsBuilder resultsBuilder) throws Exception {
        CommandScope commandScope = resultsBuilder.getCommandScope();
        String outputDir = commandScope.getArgumentValue(OUTPUT_DIR_ARG);
        for (CommandDefinition commandDefinition : getCommands()) {

            //Don't generate protobuf for generateProtobuf command step
            if (commandDefinition.getName() == COMMAND_NAME) {
                continue;
            }

            String commandName = commandDefinition.getName()[0];
            if (commandDefinition.getName().length > 1) {
                commandName += "_" + commandDefinition.getName()[1];
            }
            System.out.println(commandName);

            // Files should be named lower_snake_case.proto
            // https://developers.google.com/protocol-buffers/docs/style#file_structure
            commandName = toSnakeCase(commandName);
            String uCommandName = StringUtil.upperCaseFirst(StringUtil.toCamelCase(commandName));

            String fileName = commandName + ".proto";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "/" + fileName))) {
                writer.write("syntax = \"proto3\";\n");
                writer.write("\n");
                writer.write("option go_package=\"./;proto\";\n"); //TODO get proper golang package path
                writer.write("option java_package = \"org.liquibase.grpc.proto\";\n");
                writer.write("option java_multiple_files = true;\n");
                writer.write("option java_outer_classname = \"" + uCommandName + "Proto\";\n");
                writer.write("\n");
                writer.write("/* " + commandDefinition.getShortDescription() + " */");
                writer.write("\n");
                writer.write("package " + commandName + ";\n");
                writer.write("\n");
                writer.write("service " + uCommandName + "Service {\n");
                writer.write("  rpc execute(" + uCommandName + "Request) returns (Response) {}\n");
                writer.write("}\n");
                writer.write("\n");
                writer.write("message " + uCommandName + "Request {\n");
                String optional = "  optional";
                int i=1;
                Map<String, CommandArgumentDefinition<?>> arguments = commandDefinition.getArguments();
                for (Map.Entry<String, CommandArgumentDefinition<?>> entry : arguments.entrySet()) {
                    String dataTypeName = entry.getValue().getDataType().getSimpleName();
                    String argumentName = entry.getKey();
                    if (dataTypeName.equalsIgnoreCase("string")) {
                        writer.write(optional + " string " + argumentName + " = " + Integer.toString(i) + ";");
                    } else if (dataTypeName.equalsIgnoreCase("boolean")) {
                        writer.write(optional + " bool " + argumentName + " = " + Integer.toString(i) + ";");
                    } else if (dataTypeName.equalsIgnoreCase("Integer")) {
                        writer.write(optional + " int32 " + argumentName + " = " + Integer.toString(i) + ";");
                    } else {
                        writer.write(optional + " string " + argumentName + " = " + Integer.toString(i) + ";");
                    }
                    String required = entry.getValue().isRequired() ? "*required* " : "";
                    writer.write(" // " + required + entry.getValue().getDescription() + "\n");
                    i++;
                }
                writer.write( "  map<string, string> configuration = " + i + ";\n");
                writer.write("}\n");
                writer.write("\n");
                writer.write("message Response {\n");
                writer.write("  string message = 1;\n");
                writer.write("}\n");
            }
        }
        getConfigurations();
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
                result
                        = result
                        + Character.toLowerCase(ch);
            }
            else {
                result = result + ch;
            }
        }
        return result;
    }

    private SortedSet<CommandDefinition> getCommands() {
        final CommandFactory commandFactory = getCurrentScope().getSingleton(CommandFactory.class);
        return commandFactory.getCommands(false);
    }

    private SortedSet<ConfigurationDefinition<?>> getConfigurations() {
        LiquibaseConfiguration liquibaseConfiguration = Scope.getCurrentScope().getSingleton(LiquibaseConfiguration.class);
        SortedSet<ConfigurationDefinition<?>> definitions = liquibaseConfiguration.getRegisteredDefinitions(false);
        for (ConfigurationDefinition<?> definition : definitions) {
           System.out.println(definition.getKey() + " " + definition.getDataType() + " " + definition.getCurrentValue());
        }
        return definitions;
    }
}
