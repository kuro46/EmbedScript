package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.util.MojangUtil;
import com.github.kuro46.embedscript.util.Scheduler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public class ScriptUI {
    private static final int UNFOCUSED_CHAT_HEIGHT = 10;
    private static final int CHAT_WIDTH = 50;
    private final ScriptManager scriptManager;
    private final Cache<Player, IntConsumer> pageManager = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .weakKeys()
        .build();

    public ScriptUI(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public void embed(CommandSender sender,
                      ScriptPosition position,
                      Script script) {
        Objects.requireNonNull(script);

        if (!scriptManager.get(position).isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script already exists in that place.");
            return;
        }

        scriptManager.put(position, script);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully embedded.");
    }

    public void add(CommandSender sender,
                    ScriptPosition position,
                    Script script) {
        Objects.requireNonNull(script);

        if (scriptManager.get(position).isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        scriptManager.put(position, script);

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully added.");
    }

    public void remove(CommandSender sender, ScriptPosition position) {
        if (scriptManager.remove(position) == null) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }

        sender.sendMessage(Prefix.SUCCESS_PREFIX + "Script was successfully removed.");
    }

    public void view(CommandSender sender, ScriptPosition position) {
        List<Script> scripts = scriptManager.get(position);
        if (scripts.isEmpty()) {
            sender.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in that place.");
            return;
        }
        Scheduler.execute(() -> {
            sender.sendMessage("Script information: ------------------------------");
            for (Script script : scripts) {
                sender.sendMessage("===============================================");
                UUID author = script.getAuthor();
                Player player = Bukkit.getPlayer(author);
                String stringAuthor = player == null
                    ? MojangUtil.getName(author)
                    : player.getName();
                sender.sendMessage("author " + stringAuthor);
                sender.sendMessage("@listen-move " + collectionToString(script.getMoveTypes()));
                sender.sendMessage("@listen-click " + collectionToString(script.getClickTypes()));
                sender.sendMessage("@listen-push " + collectionToString(script.getPushTypes()));
                sender.sendMessage("@give-permission " + collectionToString(script.getPermissionsToGive()));
                sender.sendMessage("@enough-permission " + collectionToString(script.getPermissionsToNeeded()));
                sender.sendMessage("@not-enough-permission " + collectionToString(script.getPermissionsToNotNeeded()));
                sender.sendMessage("@action-type " + collectionToString(script.getActionTypes()));
                sender.sendMessage("@action " + collectionToString(script.getActions()));
                sender.sendMessage("===============================================");
            }
            sender.sendMessage("Script information: ------------------------------");
        });
    }

    private String collectionToString(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "NONE";
        } else if (collection.size() == 1) {
            return collection.iterator().next().toString();
        } else {
            return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
        }
    }

    //TODO: EDIT OPERATION

    public boolean hasScript(ScriptPosition position) {
        return scriptManager.contains(position);
    }

    private boolean isFilterable(Script script, Script filter) {
        if (isFilterable(script.getMoveTypes(), filter.getMoveTypes())) {
            return true;
        }
        if (isFilterable(script.getClickTypes(), filter.getClickTypes())) {
            return true;
        }
        if (isFilterable(script.getPushTypes(), filter.getPushTypes())) {
            return true;
        }
        if (isFilterable(script.getActionTypes(), filter.getActionTypes())) {
            return true;
        }

        BiPredicate<String, String> stringPredicate = String::equalsIgnoreCase;
        if (isFilterable(script.getActions(), filter.getActions(), stringPredicate)) {
            return true;
        }
        if (isFilterable(script.getPermissionsToGive(), filter.getPermissionsToGive(), stringPredicate)) {
            return true;
        }
        if (isFilterable(script.getPermissionsToNeeded(), filter.getPermissionsToNeeded(), stringPredicate)) {
            return true;
        }

        return isFilterable(script.getPermissionsToNotNeeded(), filter.getPermissionsToNotNeeded(), stringPredicate);
    }

    private <E> boolean isFilterable(Collection<E> target, Collection<E> filter, BiPredicate<E, E> equals) {
        for (E f : filter) {
            if (target.isEmpty()) {
                return true;
            }
            for (E t : target) {
                if (!equals.test(f, t)) {
                    return true;
                }
            }
        }
        return false;
    }

    private <E> boolean isFilterable(Collection<E> target, Collection<E> filter) {
        return isFilterable(target, filter, Object::equals);
    }

    /**
     * Send list of scripts to player
     *
     * @param player    Player
     * @param world     World (Nullable)
     * @param pageIndex page index
     */
    public void list(Player player, String world, Script filter, int pageIndex) {
        Map<ScriptPosition, List<Script>> snapshot = scriptManager.snapshot();
        Scheduler.execute(() -> {
            Map<ScriptPosition, List<Script>> sorted = new TreeMap<>((position, position1) -> {
                int worldCompareTo = position.getWorld().compareTo(position1.getWorld());
                if (worldCompareTo != 0) {
                    return worldCompareTo;
                }
                int yCompareTo = Integer.compare(position.getY(), position1.getY());
                if (yCompareTo != 0) {
                    return yCompareTo;
                }
                int xCompareTo = Integer.compare(position.getX(), position1.getX());
                if (xCompareTo != 0) {
                    return xCompareTo;
                }

                return Integer.compare(position.getZ(), position1.getZ());
            });
            sorted.putAll(snapshot);

            List<BaseComponent[]> messages = new ArrayList<>();
            for (Map.Entry<ScriptPosition, List<Script>> entry : sorted.entrySet()) {
                ScriptPosition position = entry.getKey();
                List<Script> scripts = entry.getValue();

                for (Script script : scripts) {
                    if ((filter != null && isFilterable(script, filter)) ||
                        (world != null && !world.equals("all") && !world.equalsIgnoreCase(position.getWorld()))) {
                        continue;
                    }

                    String tpCommand = "/embedscript teleport " + position.getWorld() + " " + position.getX() + " "
                        + position.getY() + " " + position.getZ();
                    BaseComponent[] message = new ComponentBuilder("")
                        .append("[" + (messages.size() + 1) + "] ")
                        .append("World: " + position.getWorld() + " X: " + position.getX()
                            + " Y: " + position.getY() + " Z: " + position.getZ() + " (click here)")
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tpCommand)))
                        .create();
                    messages.add(message);
                }
            }
            if (messages.isEmpty()) {
                player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists.");
            } else {
                String target = world == null || world.equals("all")
                    ? "this server"
                    : world;
                sendPage("List of scripts in " + target,
                    player,
                    messages,
                    pageIndex);
            }
        });
    }

    public void changePage(Player player, int pageIndex) {
        IntConsumer consumer = pageManager.getIfPresent(player);
        if (consumer == null) {
            player.sendMessage(Prefix.ERROR_PREFIX + "Cannot get your page.");
            return;
        }
        consumer.accept(pageIndex);
    }

    private void sendPage(String title,
                          Player player,
                          List<BaseComponent[]> messages,
                          int pageIndex) {
        sendPage(title, player, messages, pageIndex, UNFOCUSED_CHAT_HEIGHT);
    }

    private void sendPage(String title,
                          Player player,
                          List<BaseComponent[]> messages,
                          int pageIndex,
                          int chatHeight) {
        int availableMessageHeight = chatHeight - 3;
        List<List<BaseComponent[]>> pages = new ArrayList<>();
        List<BaseComponent[]> buffer = new ArrayList<>();
        for (BaseComponent[] message : messages) {
            buffer.add(message);
            if (buffer.size() >= availableMessageHeight) {
                pages.add(new ArrayList<>(buffer));
                buffer.clear();
            }
        }
        List<BaseComponent[]> lastPage = new ArrayList<>(buffer);
        //last page pad with space
        int padLines = availableMessageHeight - lastPage.size();
        for (int i = 0; i < padLines; i++) {
            lastPage.add(TextComponent.fromLegacyText(""));
        }
        pages.add(lastPage);

        if (pageIndex >= pages.size() || pageIndex < 0) {
            player.sendMessage("Out of bounds");
            return;
        }
        List<BaseComponent[]> page = pages.get(pageIndex);

        String separator = "---< " + title + " >---";
        int separatorStringLength = (CHAT_WIDTH - separator.length()) / 2;
        String separatorString = StringUtils.repeat("-", separatorStringLength);
        separator = separatorString + separator + separatorString;

        player.sendMessage(separator);
        page.forEach(baseComponents -> player.spigot().sendMessage(baseComponents));

        int previousPageIndex = pageIndex - 1 < 0
            ? pages.size() - 1
            : pageIndex - 1;
        int nextPageIndex = pageIndex + 1 >= pages.size()
            ? 0
            : pageIndex + 1;
        player.spigot().sendMessage(new ComponentBuilder("")
            .append(new ComponentBuilder("<<Previous>>")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/embedscript page " + previousPageIndex))
                .create())
            .append("   ")
            .append(new ComponentBuilder("<<Next>>")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/embedscript page " + nextPageIndex))
                .create())
            .append("   ")
            .append(new ComponentBuilder(String.format("<<Page %d of %d>>", pageIndex + 1, pages.size()))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ""))
                .create())
            .create());
        player.sendMessage(separator);

        pageManager.put(player, value -> sendPage(title, player, messages, value, chatHeight));
    }
}
