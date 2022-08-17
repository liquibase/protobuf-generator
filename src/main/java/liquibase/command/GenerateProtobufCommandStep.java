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
                .defaultValue("src/main/proto")
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
            String commandName = commandDefinition.getName()[0];
            if (commandDefinition.getName().length > 1) {
                commandName += "_" + commandDefinition.getName()[1];
            }
            System.out.println(commandName);
            commandName = StringUtil.upperCaseFirst(commandName);
            String fileName = commandName + ".proto";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "/" + fileName))) {
                writer.write("syntax = \"proto3\";\n");
                writer.write("\n");
                // writer.write("option go_package=\"./pb;command\";");
                writer.write("option go_package=\"./;proto\";");
                writer.write("\n");
                writer.write("option java_package = \"com.command.proto\";\n");
                writer.write("\n");
                writer.write("option java_multiple_files = true;\n");
                writer.write("option java_outer_classname = \"" + commandName + "Proto\";\n");

                writer.write("package " + commandName + ";\n");
                writer.write("\n");
                writer.write("service " + commandName + "Service {\n");
                writer.write("  rpc execute(" + commandName + "Request) returns (" + commandName + "Response) {}\n");
                writer.write("}\n");

                writer.write("message " + commandName + "Request {\n");
                String optional = "  optional";
                int i=1;
                Map<String, CommandArgumentDefinition<?>> arguments = commandDefinition.getArguments();
                for (Map.Entry<String, CommandArgumentDefinition<?>> entry : arguments.entrySet()) {
                    String dataTypeName = entry.getValue().getDataType().getSimpleName();
                    String argumentName = entry.getKey();
                    if (dataTypeName.equalsIgnoreCase("string")) {
                        writer.write(optional + " string " + argumentName + " = " + Integer.toString(i) + ";\n");
                    } else if (dataTypeName.equalsIgnoreCase("boolean")) {
                        writer.write(optional + " bool " + argumentName + " = " + Integer.toString(i) + ";\n");
                    } else if (dataTypeName.equalsIgnoreCase("Integer")) {
                        writer.write(optional + " int32 " + argumentName + " = " + Integer.toString(i) + ";\n");
                    } else {
                        writer.write(optional + " string " + argumentName + " = " + Integer.toString(i) + ";\n");
                    }
                    i++;
                }
                writer.write( "  map<string, string> Configuration = " + i + ";\n");
                writer.write("}\n");

                writer.write("message " + commandName + "Response {\n");
                writer.write("  string message = 1;\n");
                writer.write("}\n");
            }
        }
        getConfigurations();
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
