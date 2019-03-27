package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GivePermissionProcessor implements Processor {
    private final Executor executor;
    private final Parser parser;

    public GivePermissionProcessor(Plugin plugin, Configuration configuration) {
        this.executor = new GivePermissionExecutor(plugin);
        this.parser = new GivePermissionParser(configuration);
    }

    @Override
    public String getKey() {
        return "give-permission";
    }

    @Override
    public String getOmittedKey() {
        return "gp";
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    private static class GivePermissionExecutor extends AbstractExecutor {
        private final Map<Player, PermissionAttachment> attachments = new HashMap<>();
        private final Plugin plugin;

        public GivePermissionExecutor(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void prepareExecute(Player trigger, ImmutableList<String> matchedValues) {
            PermissionAttachment attachment = matchedValues.isEmpty() ? null : trigger.addAttachment(plugin);
            for (String matchedValue : matchedValues) {
                if (trigger.hasPermission(matchedValue)) {
                    continue;
                }
                attachment.setPermission(matchedValue, true);
            }
            attachments.put(trigger, attachment);
        }

        @Override
        public void endExecute(Player trigger, ImmutableList<String> matchedValues) {
            PermissionAttachment attachment = attachments.get(trigger);
            if (attachment != null) {
                attachment.remove();
            }
        }
    }

    private static class GivePermissionParser extends AbstractParser {
        private final Configuration configuration;

        public GivePermissionParser(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void build(ScriptBuilder builder, String key, ImmutableList<String> matchedValues) throws ParseException {
            if (!matchedValues.isEmpty()) {
                builder.getScript().putAll(key, matchedValues);
            } else {
                Set<String> preferPermissions = new HashSet<>();
                Map<String, List<String>> permissionsForActions = configuration.getPermissionsForActions();
                for (String action : builder.getScript().values()) {
                    List<String> permissionsForAction = permissionsForActions.get(action);

                    if (permissionsForAction == null) {
                        int skipElement = 1;
                        while (permissionsForAction == null) {
                            String[] split = action.split(" ");
                            ArrayUtils.reverse(split);
                            String[] skipped = Arrays.stream(split)
                                .skip(skipElement)
                                .toArray(String[]::new);
                            if (skipped.length == 0) {
                                break;
                            }
                            ArrayUtils.reverse(skipped);
                            permissionsForAction = permissionsForActions.get(String.join(" ", skipped));

                            skipElement++;
                        }
                    }

                    if (permissionsForAction == null) {
                        continue;
                    }
                    preferPermissions.addAll(permissionsForAction);
                }
                builder.getScript().putAll(key, preferPermissions);
            }
        }
    }
}
