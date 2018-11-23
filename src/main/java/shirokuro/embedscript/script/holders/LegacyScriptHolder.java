package shirokuro.embedscript.script.holders;

import com.google.gson.annotations.JsonAdapter;
import shirokuro.embedscript.script.adapters.IScriptHolderAdapter;

/**
 * @author shirokuro
 */
@JsonAdapter(IScriptHolderAdapter.class)
public class LegacyScriptHolder implements IScriptHolder {
    //DUMMY
    public static final String FORMAT_VERSION = "0.1";

    @Override
    public String formatVersion() {
        return FORMAT_VERSION;
    }
}
