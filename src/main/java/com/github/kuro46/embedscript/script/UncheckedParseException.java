package com.github.kuro46.embedscript.script;

public class UncheckedParseException extends RuntimeException {
    public UncheckedParseException(ParseException cause) {
        super(cause);
    }

    @Override
    public synchronized ParseException getCause() {
        return (ParseException) super.getCause();
    }
}
