package com.github.kuro46.embedscript.script.processor;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptPosition;
import com.github.kuro46.embedscript.script.ScriptUtil;
import com.github.kuro46.embedscript.util.Scheduler;
import com.github.kuro46.embedscript.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScriptProcessor {
    private final Map<String, Processor> processors = new HashMap<>();
    private final Configuration configuration;
    private final Logger logger;

    public ScriptProcessor(Logger logger, Plugin plugin, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;

        addProcessor(Processors.LISTEN_CLICK_PROCESSOR);
        addProcessor(Processors.LISTEN_MOVE_PROCESSOR);
        addProcessor(Processors.LISTEN_PUSH_PROCESSOR);
        addProcessor(Processors.BROADCAST_PROCESSOR);
        addProcessor(Processors.BROADCAST_JSON_PROCESSOR);
        addProcessor(Processors.SAY_PROCESSOR);
        addProcessor(Processors.SAY_JSON_PROCESSOR);
        addProcessor(Processors.NEEDED_PERMISSION_PROCESSOR);
        addProcessor(Processors.UNNEEDED_PERMISSION_PROCESSOR);
        addProcessor(Processors.COMMAND_PROCESSOR);
        addProcessor(Processors.CONSOLE_PROCESSOR);
        addProcessor(new GivePermissionProcessor(plugin, configuration));
        addProcessor(new PresetProcessor(configuration));
    }

    public Processor addProcessor(Processor processor) {
        return processors.put(processor.getKey(), processor);
    }

    // PARSE START

    public Script parse(UUID author, String script) throws ParseException {
        MutableScript mutableScript = new MutableScript(script);
        prepareBuild(mutableScript);
        return buildScript(author, mutableScript);
    }

    /**
     * Prepare to build
     *
     * @param script script that ready to build
     * @throws ParseException If parse failed
     */
    public void prepareBuild(MutableScript script) throws ParseException {
        canonicalizeBuffer(script);
        ImmutableListMultimap<String, String> view = script.getView();
        for (String key : view.keySet()) {
            Processor processor = processors.get(key);
            processor.getParser().prepareBuild(this, script, key, view.get(key));
        }
    }

    /**
     * Canonicalize the keys e.g. 'p' to 'preset'
     *
     * @param script script to canonicalize
     * @throws ParseException If processor for key not exists
     */
    private void canonicalizeBuffer(MutableScript script) throws ParseException {
        // generate lookup table
        Map<String, String> omittedKeys = new HashMap<>();
        Set<String> keys = new HashSet<>();
        for (Processor processor : processors.values()) {
            omittedKeys.put(processor.getOmittedKey(), processor.getKey());
            keys.add(processor.getKey());
        }

        for (Map.Entry<String, String> entry : script.getView().entries()) {
            if (omittedKeys.containsKey(entry.getKey())) {
                script.replaceKey(entry.getValue(), entry.getKey(), omittedKeys.get(entry.getKey()));
                continue;
            }

            if (!keys.contains(entry.getKey())) {
                throw new ParseException(String.format("'%s' is unknown key!", entry.getKey()));
            }
        }
    }

    /**
     * Build the script from MutableScript
     *
     * @param author author of this script
     * @param script modifiable script
     * @return script
     */
    private Script buildScript(UUID author, MutableScript script) throws ParseException {
        ScriptBuilder builder = ScriptBuilder.withAuthor(author);
        ImmutableListMultimap<String, String> view = script.getView();
        for (Map.Entry<String, String> entry : view.entries()) {
            String key = entry.getKey();
            Processor processor = processors.get(key);
            processor.getParser().build(builder, key, view.get(key));
        }

        return builder.build();
    }

    // EXECUTE START

    public void execute(Player trigger, Script script, ScriptPosition scriptPosition) {
        Map<Processor.Executor, ImmutableList<String>> executors = new HashMap<>();
        ImmutableListMultimap<String, String> scriptMap = script.getScript();
        for (Processor processor : processors.values()) {
            if (!scriptMap.containsKey(processor.getKey())) {
                continue;
            }

            List<String> value = scriptMap.get(processor.getKey()).stream()
                .map(string -> {
                    string = replaceAndUnescape(string, "<player>", trigger::getName);
                    string = replaceAndUnescape(string, "<world>", () -> trigger.getWorld().getName());
                    return string;
                })
                .collect(Collectors.toList());
            executors.put(processor.getExecutor(), ImmutableList.copyOf(value));
        }

        try {
            // check phase
            for (Map.Entry<Processor.Executor, ImmutableList<String>> entry : executors.entrySet()) {
                if (!entry.getKey().check(trigger, entry.getValue())) {
                    return;
                }
            }

            // prepare phase
            executors.forEach((executor, matchedValues) -> executor.prepareExecute(trigger, matchedValues));

            // execute start
            executors.forEach((executor, matchedValues) -> executor.beginExecute(trigger, matchedValues));
        } finally {
            // execute end
            executors.forEach((executor, matchedValues) -> executor.endExecute(trigger, matchedValues));
        }

        if (configuration.isLogEnabled()) {
            Scheduler.execute(() -> {
                String message = configuration.getLogFormat();
                message = replaceAndUnescape(message, "<trigger>", trigger::getName);
                message = replaceAndUnescape(message, "<script>", () -> {
                    StringJoiner joiner = new StringJoiner(" ");
                    for (String key : scriptMap.keySet()) {
                        joiner.add('@' + key + ' ' + ScriptUtil.toString(scriptMap.get(key)));
                    }
                    return joiner.toString();
                });
                Location location = trigger.getLocation();
                String worldName = location.getWorld().getName();
                message = replaceAndUnescape(message, "<trigger_world>", () -> worldName);
                message = replaceAndUnescape(message, "<trigger_x>", () -> toString(location.getBlockX()));
                message = replaceAndUnescape(message, "<trigger_y>", () -> toString(location.getBlockY()));
                message = replaceAndUnescape(message, "<trigger_z>", () -> toString(location.getBlockZ()));
                message = replaceAndUnescape(message, "<script_world>", () -> worldName);
                message = replaceAndUnescape(message, "<script_x>", () -> toString(scriptPosition.getX()));
                message = replaceAndUnescape(message, "<script_y>", () -> toString(scriptPosition.getY()));
                message = replaceAndUnescape(message, "<script_z>", () -> toString(scriptPosition.getZ()));

                logger.info(message);
            });
        }
    }

    private String replaceAndUnescape(String source, String target, Supplier<String> messageFactory) {
        if (!source.contains(target)) {
            return source;
        }

        return Util.replaceAndUnescape(source, target, messageFactory.get());
    }

    private String toString(Object o) {
        return o.toString();
    }
}
