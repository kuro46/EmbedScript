package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.Configuration;
import com.github.kuro46.embedscript.script.ScriptBuffer;
import com.github.kuro46.embedscript.script.ScriptBuilder;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GivePermissionProcessor extends AbstractProcessor {
    private final Configuration configuration;

    public GivePermissionProcessor(Configuration configuration) {
        super("give-permission", "gp");
        this.configuration = configuration;
    }

    @Override
    public boolean allowEmptyList() {
        return true;
    }

    @Override
    public List<Class<? extends Processor>> getDepends(Phase phase) {
        if (phase == Phase.PROCESS) {
            return Collections.singletonList(Processors.ACTION.getClass());
        } else {
            return super.getDepends(phase);
        }
    }

    @Override
    public void process(ScriptParser parser,
                        ScriptBuilder builder,
                        ScriptBuffer source,
                        String key,
                        List<String> values) {
        if (!values.isEmpty()) {
            builder.withPermissionsToGive(values.toArray(new String[0]));
        } else {
            Set<String> preferPermissions = new HashSet<>();
            Map<String, List<String>> permissionsForActions = configuration.getPermissionsForActions();
            for (String action : builder.getActions()) {
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
            builder.withPermissionsToGive(preferPermissions.toArray(new String[0]));
        }
    }
}
