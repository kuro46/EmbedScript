package com.github.kuro46.embedscript.script

class MutableEntry(
        override val values: MutableList<String>,
        override val children: MutableMap<String, MutableEntry>
) : Entry(values, children)
