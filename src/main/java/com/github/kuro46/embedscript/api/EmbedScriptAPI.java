package com.github.kuro46.embedscript.api;

import com.github.kuro46.embedscript.script.processor.ScriptProcessor;

public class EmbedScriptAPI {
    private final ScriptProcessor scriptProcessor;

    public EmbedScriptAPI(ScriptProcessor scriptProcessor) {
        this.scriptProcessor = scriptProcessor;
    }

    public ScriptProcessor getScriptProcessor() {
        return scriptProcessor;
    }
}
