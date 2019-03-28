package com.github.kuro46.embedscript.script

/**
 * @author shirokuro
 */
enum class EventType(val fileName: String, val commandName: String, val presetName: String) {
    WALK("WalkScripts.json", "eswalk", "walk"),
    INTERACT("InteractScripts.json", "esinteract", "interact")
}
