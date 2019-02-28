package com.github.kuro46.embedscript.script.parser;

import com.github.kuro46.embedscript.script.ParseException;
import com.github.kuro46.embedscript.script.Script;
import com.github.kuro46.embedscript.script.ScriptBuffer;
import com.github.kuro46.embedscript.script.UncheckedParseException;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

public class Processors {
    private Processors(){

    }

    public static final Processor LISTEN_CLICK = new AbstractProcessor("listen-click","lc") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withClickTypes(stringListToEnumArray(Script.ClickType.class,values, Script.ClickType[]::new));
        }
    };
    
    public static final Processor LISTEN_MOVE = new AbstractProcessor("listen-move","lm") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withMoveTypes(stringListToEnumArray(Script.MoveType.class,values, Script.MoveType[]::new));
        }
    };
    
    public static final Processor LISTEN_PUSH = new AbstractProcessor("listen-push","lp") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withPushTypes(stringListToEnumArray(Script.PushType.class,values, Script.PushType[]::new));
        }
    };

    public static final Processor ENOUGH_PERMISSION = new AbstractProcessor("enough-permission","ep") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withPermissionsToNeeded(stringListToArray(values));
        }
    };

    public static final Processor NOT_ENOUGH_PERMISSION = new AbstractProcessor("not-enough-permission","nep") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withPermissionsToNotNeeded(stringListToArray(values));
        }
    };

    public static final Processor GIVE_PERMISSION = new AbstractProcessor("give-permission","gp") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withPermissionsToGive(stringListToArray(values));
        }
    };

    public static final Processor ACTION_TYPE = new AbstractProcessor("action-type","at") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) throws ParseException {
            builder.withActionTypes(stringListToEnumArray(Script.ActionType.class,values, Script.ActionType[]::new));
        }
    };

    public static final Processor ACTION = new AbstractProcessor("action","a") {
        @Override
        public void process(Script.Builder builder, ScriptBuffer source, String key, List<String> values) {
            builder.withActions(stringListToArray(values));
        }
    };

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
        }catch (UncheckedParseException e){
            throw e.getCause();
        }
    }

    private static String[] stringListToArray(List<String> list){
        return list.toArray(new String[0]);
    }
}
