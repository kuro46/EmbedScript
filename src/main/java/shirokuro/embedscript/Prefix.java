package shirokuro.embedscript;

import org.bukkit.ChatColor;

/**
 * @author shirokuro
 */
public final class Prefix {
    public static final String PREFIX = ChatColor.GRAY + "<"
        + ChatColor.DARK_GREEN + "E" + ChatColor.GREEN + "S" + ChatColor.GRAY + "> ";
    public static final String WARN_PREFIX = PREFIX + ChatColor.GOLD;
    public static final String ERROR_PREFIX = PREFIX + ChatColor.RED;
    public static final String SUCCESS_PREFIX = PREFIX + ChatColor.GREEN;

    private Prefix() {
        throw new UnsupportedOperationException("Constant class.");
    }
}
