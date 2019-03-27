package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Processors {
    public static final Processor.Executor DEFAULT_EXECUTOR = new AbstractExecutor() {
    };
    public static final Processor.Parser DEFAULT_PARSER = new AbstractParser() {
    };

    // ONLY TO PARSE
    public static final Processor LISTEN_CLICK_PROCESSOR = newProcessor("listen-click", "lc",
        new AbstractParser() {
            @Override
            public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
                addEnumToCollection(builder.getClickTypes(), Script.ClickType.class, matchedValues);
            }
        },
        DEFAULT_EXECUTOR);
    public static final Processor LISTEN_MOVE_PROCESSOR = newProcessor("listen-move", "lm",
        new AbstractParser() {
            @Override
            public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
                addEnumToCollection(builder.getMoveTypes(), Script.MoveType.class, matchedValues);
            }
        },
        DEFAULT_EXECUTOR);
    public static final Processor LISTEN_PUSH_PROCESSOR = newProcessor("listen-push", "lm",
        new AbstractParser() {
            @Override
            public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
                addEnumToCollection(builder.getPushTypes(), Script.PushType.class, matchedValues);
            }
        },
        DEFAULT_EXECUTOR);

    // CHECK PHASE

    public static final Processor NEEDED_PERMISSION_PROCESSOR = newProcessor("needed-permission", "np",
        DEFAULT_PARSER,
        new NeededPermissionExecutor());
    public static final Processor UNNEEDED_PERMISSION_PROCESSOR = newProcessor("unneeded-permission", "up",
        DEFAULT_PARSER,
        new NeededPermissionExecutor() {
            @Override
            public boolean check(Player trigger, ImmutableList<String> matchedValues) {
                // invert
                return !super.check(trigger, matchedValues);
            }
        });

    // EXECUTION PHASE

    private static final Processor.Parser COMMAND_PARSER = new AbstractParser() {
        @Override
        public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) {
            List<String> modifiedForCommand = matchedValues.stream()
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
                .collect(Collectors.toList());
            builder.getScript().putAll(key, modifiedForCommand);
        }
    };

    public static final Processor COMMAND_PROCESSOR = newProcessor("command", "c",
        COMMAND_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                matchedValues.forEach(trigger::performCommand);
            }
        });
    public static final Processor CONSOLE_PROCESSOR = newProcessor("console", "con",
        COMMAND_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                matchedValues.forEach(string -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string));
            }
        });
    public static final Processor SAY_PROCESSOR = newProcessor("say", "s",
        DEFAULT_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                matchedValues.forEach(trigger::sendMessage);
            }
        });
    public static final Processor SAY_JSON_PROCESSOR = newProcessor("say-json", "sj",
        DEFAULT_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                matchedValues.forEach(string -> trigger.spigot().sendMessage(ComponentSerializer.parse(string)));
            }
        });
    public static final Processor BROADCAST_PROCESSOR = newProcessor("broadcast", "b",
        DEFAULT_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    for (String string : matchedValues) {
                        player.sendMessage(string);
                    }
                });
            }
        });
    public static final Processor BROADCAST_JSON_PROCESSOR = newProcessor("broadcast-json", "bj",
        DEFAULT_PARSER,
        new AbstractExecutor() {
            @Override
            public void beginExecute(Player trigger, ImmutableList<String> matchedValues) {
                List<BaseComponent[]> jsonMessages = matchedValues.stream()
                    .map(ComponentSerializer::parse)
                    .collect(Collectors.toList());
                Bukkit.getOnlinePlayers().forEach(player ->
                    jsonMessages.forEach(baseComponents -> player.spigot().sendMessage(baseComponents)));
            }
        });

    private Processors() {
    }

    public static Processor newProcessor(String key,
                                         String omittedKey,
                                         Processor.Parser parser,
                                         Processor.Executor executor) {
        return new Processor() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getOmittedKey() {
                return omittedKey;
            }

            @Override
            public Parser getParser() {
                return parser;
            }

            @Override
            public Executor getExecutor() {
                return executor;
            }
        };
    }

    private static <T extends Enum<T>> void addEnumToCollection(Collection<T> collection,
                                                                Class<T> clazz,
                                                                List<String> strings) throws ParseException {
        for (String string : strings) {
            try {
                collection.add(Enum.valueOf(clazz, string.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                throw new ParseException(String.format("'%s' is unavailable value!", string));
            }
        }
    }

    private static class NeededPermissionExecutor extends AbstractExecutor {
        @Override
        public boolean check(Player trigger, ImmutableList<String> matchedValues) {
            for (String matchedValue : matchedValues) {
                if (!trigger.hasPermission(matchedValue)) {
                    return false;
                }
            }
            return true;
        }
    }
}
