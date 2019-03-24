package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;
import com.github.kuro46.embedscript.script.ScriptBuilder;
import com.github.kuro46.embedscript.script.UncheckedParseException;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class Processors {
    public static final Processor LISTEN_CLICK = new AbstractProcessor("listen-click", "lc") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withClickTypes(stringListToEnumArray(Script.ClickType.class, values, Script.ClickType[]::new));
        }
    };
    public static final Processor LISTEN_MOVE = new AbstractProcessor("listen-move", "lm") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withMoveTypes(stringListToEnumArray(Script.MoveType.class, values, Script.MoveType[]::new));
        }
    };
    public static final Processor LISTEN_PUSH = new AbstractProcessor("listen-push", "lp") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withPushTypes(stringListToEnumArray(Script.PushType.class, values, Script.PushType[]::new));
        }
    };
    public static final Processor NEEDED_PERMISSION = new AbstractProcessor("needed-permission", "np") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withNeededPermissions(stringListToArray(values));
        }
    };
    public static final Processor UNNEEDED_PERMISSION = new AbstractProcessor("unneeded-permission", "up") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withUnneededPermissions(stringListToArray(values));
        }
    };
    public static final Processor ACTION_TYPE = new AbstractProcessor("action-type", "at") {
        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withActionTypes(stringListToEnumArray(Script.ActionType.class, values, Script.ActionType[]::new));
        }
    };
    public static final Processor ACTION = new AbstractProcessor("action", "a") {
        @Override
        public List<Class<? extends Processor>> getDepends(Phase phase) {
            if (phase == Phase.PROCESS) {
                return Collections.singletonList(ACTION_TYPE.getClass());
            } else {
                return super.getDepends(phase);
            }
        }

        @Override
        public void process(ScriptParser parser, ScriptBuilder builder, ScriptBuffer source, String key, List<String> values) {
            String[] arrayValues = stringListToArray(values);

            if (!ArrayUtils.contains(builder.getActionTypes(), Script.ActionType.COMMAND)) {
                builder.withActions(arrayValues);
            } else {
                String[] modifiedForCommand = Arrays.stream(arrayValues)
                    // remove slash char if needed
                    .map(commandWithArgs -> commandWithArgs.startsWith("/")
                        ? commandWithArgs.substring(1)
                        : commandWithArgs)
                    // canonicalize the command
                    .map(commandWithArgs -> {
                        String[] splitCommandWithArgs = commandWithArgs.split(" ");
                        PluginCommand pluginCommand = Bukkit.getPluginCommand(splitCommandWithArgs[0]);
                        String canonicalizedCommand = pluginCommand == null
                            ? splitCommandWithArgs[0]
                            : pluginCommand.getName();
                        String args = Arrays.stream(splitCommandWithArgs)
                            .skip(1)
                            .collect(Collectors.joining(" "));
                        return canonicalizedCommand + " " + args;
                    })
                    .toArray(String[]::new);

                builder.withActions(modifiedForCommand);
            }
        }
    };

    private Processors() {

    }

    private static <E extends Enum<E>> E[] stringListToEnumArray(Class<E> enumClass,
                                                                 List<String> stringList,
                                                                 IntFunction<E[]> arrayFunction) throws ParseException {
        try {
            return stringList.stream()
                .map(s -> {
                    try {
                        return Enum.valueOf(enumClass, s.toUpperCase(Locale.ENGLISH));
                    } catch (IllegalArgumentException e) {
                        throw new UncheckedParseException(
                            new ParseException(String.format("'%s' is unknown value.", s)));
                    }
                }).toArray(arrayFunction);
        } catch (UncheckedParseException e) {
            throw e.getCause();
        }
    }

    private static String[] stringListToArray(List<String> list) {
        return list.toArray(new String[0]);
    }
}
