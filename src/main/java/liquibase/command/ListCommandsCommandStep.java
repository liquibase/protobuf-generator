package liquibase.command;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static liquibase.Scope.getCurrentScope;

public class ListCommandsCommandStep extends AbstractCommandStep {

    public static final String[] COMMAND_NAME = {"listCommands"};
    public static final CommandArgumentDefinition<String> EDITION;

    static {
        CommandBuilder builder = new CommandBuilder(ListCommandsCommandStep.COMMAND_NAME);
        EDITION = builder.argument("edition", String.class)
                .defaultValue("")
                .description("core or pro").build();
    }

    @Override
    public String[][] defineCommandNames() {
        return new String[][]{COMMAND_NAME};
    }

    @Override
    public void run(CommandResultsBuilder resultsBuilder) throws Exception {
        final CommandFactory commandFactory = getCurrentScope().getSingleton(CommandFactory.class);
        SortedSet<CommandDefinition> commands = commandFactory.getCommands(false);
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (CommandDefinition command : commands) {
            List<CommandStep> commandStep = command.getPipeline();
            String commandName = commandStep.get(0).getClass().getName();

            if (resultsBuilder.getCommandScope().getArgumentValue(EDITION).equalsIgnoreCase("pro") && commandName.contains("liquibase.command.core")) {
                continue;
            }
            if (resultsBuilder.getCommandScope().getArgumentValue(EDITION).equalsIgnoreCase("core") && commandName.contains("com.datical")) {
                continue;
            }
            if (command.getName() == COMMAND_NAME || command.getName() == GenerateProtobufCommandStep.COMMAND_NAME) {
                continue;
            }
            if (command.getHidden()) {
                continue;
            }

            StringBuilder out = new StringBuilder();
            if (command.getName().length > 1) {
                for (String s : command.getName()) {
                    out.append(toKebabCase(s)).append(" ");
                }
                int last = out.length() - 1;
                out.replace(last, last + 1, "");
                json.append("\"").append(out).append("\"").append(",");
            } else {
                for (String s : command.getName()) {
                    out.append(toKebabCase(s)).append(" ");
                }
                json.append("\"").append(out.toString().trim()).append("\"").append(",");
            }
        }
        int last = json.length() - 1;
        json.replace(last, last + 1, "");
        json.append("]");
        resultsBuilder.getOutputStream().write(json.toString().getBytes());
    }

    private String toKebabCase(String str) {
        char c = str.charAt(0);
        StringBuilder result = new StringBuilder(String.valueOf(Character.toLowerCase(c)));
        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('-');
                result.append(Character.toLowerCase(ch));
            }
            else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
