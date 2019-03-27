package com.github.kuro46.embedscript.script;

import com.github.kuro46.embedscript.Prefix;
import com.github.kuro46.embedscript.util.MojangUtil;
import com.github.kuro46.embedscript.util.Scheduler;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public class ScriptUI {
    private static final int UNFOCUSED_CHAT_HEIGHT = 10;
    private static final int CHAT_WIDTH = 50;
    private final ScriptManager scriptManager;
    private final Cache<CommandSender, IntConsumer> pageManager = CacheBuilder.newBuilder()
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
            List<BaseComponent[]> messages = new ArrayList<>();
            for (Script script : scripts) {
                UUID author = script.getAuthor();
                Player player = Bukkit.getPlayer(author);
                String stringAuthor = player == null
                    ? MojangUtil.getName(author)
                    : player.getName();
                messages.add(TextComponent.fromLegacyText("author " + stringAuthor));
                messages.add(TextComponent.fromLegacyText("@listen-move " + collectionToString(script.getMoveTypes())));
                messages.add(TextComponent.fromLegacyText("@listen-click " + collectionToString(script.getClickTypes())));
                messages.add(TextComponent.fromLegacyText("@listen-push " + collectionToString(script.getPushTypes())));
                ImmutableListMultimap<String, String> scriptMap = script.getScript();
                for (String key : scriptMap.keySet()) {
                    ImmutableList<String> value = scriptMap.get(key);
                    messages.add(TextComponent.fromLegacyText('@' + key + ' ' + collectionToString(value)));
                }
            }
            sendPage("Script information", sender, messages, 0, 12);
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
                .map(s -> s + ChatColor.RESET)
                .collect(Collectors.joining("][", "[", "]"));
        }
    }

    /**
     * Send list of scripts to player
     *
     * @param player    Player
     * @param world     World (Nullable)
     * @param pageIndex page index
     */
    public void list(Player player, String world, Script filter, int pageIndex) {
        Scheduler.execute(() -> {
            List<BaseComponent[]> messages = scriptManager.getScripts().entrySet().stream()
                .filter(entry -> world == null ||
                    world.equals("all") ||
                    world.equalsIgnoreCase(entry.getKey().getWorld()))
                .sorted(new ScriptPositionComparator())
                .collect(new ScriptCollector(filter));

            String target = world == null || world.equals("all")
                ? "this server"
                : world;

            if (messages.isEmpty()) {
                player.sendMessage(Prefix.ERROR_PREFIX + "Script not exists in " + target);
            } else {
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
                          CommandSender sender,
                          List<BaseComponent[]> messages,
                          int pageIndex) {
        sendPage(title, sender, messages, pageIndex, UNFOCUSED_CHAT_HEIGHT);
    }

    private void sendPage(String title,
                          CommandSender sender,
                          List<BaseComponent[]> messages,
                          int pageIndex,
                          int chatHeight) {
        int availableMessageHeight = chatHeight - 3;
        List<List<BaseComponent[]>> pages = splitMessages(messages, availableMessageHeight);

        if (pageIndex >= pages.size() || pageIndex < 0) {
            sender.sendMessage("Out of bounds");
            return;
        }
        List<BaseComponent[]> page = pages.get(pageIndex);

        String separator = titleToSeparator(title);
        sender.sendMessage(separator);
        page.forEach(baseComponents -> sender.spigot().sendMessage(baseComponents));

        int previousPageIndex = pageIndex - 1 < 0
            ? pages.size() - 1
            : pageIndex - 1;
        int nextPageIndex = pageIndex + 1 >= pages.size()
            ? 0
            : pageIndex + 1;
        sender.spigot().sendMessage(new ComponentBuilder("")
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
        sender.sendMessage(separator);

        pageManager.put(sender, value -> sendPage(title, sender, messages, value, chatHeight));
    }

    private String titleToSeparator(String title) {
        title = "---< " + title + " >---";
        String separatorString = StringUtils.repeat("-", (CHAT_WIDTH - title.length()) / 2);
        return separatorString + title + separatorString;
    }

    private List<List<BaseComponent[]>> splitMessages(List<BaseComponent[]> messages, int maximumLines) {
        List<List<BaseComponent[]>> pages = new ArrayList<>();
        List<BaseComponent[]> buffer = new ArrayList<>();
        for (BaseComponent[] message : messages) {
            buffer.add(message);
            if (buffer.size() >= maximumLines) {
                pages.add(new ArrayList<>(buffer));
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            List<BaseComponent[]> lastPage = new ArrayList<>(buffer);
            //last page pad with space
            int padLines = maximumLines - lastPage.size();
            for (int i = 0; i < padLines; i++) {
                lastPage.add(TextComponent.fromLegacyText(""));
            }
            pages.add(lastPage);
        }
        return pages;
    }

    @SuppressWarnings("serial")
    private static class ScriptPositionComparator implements Comparator<Map.Entry<ScriptPosition, List<Script>>>, Serializable {
        @Override
        public int compare(Map.Entry<ScriptPosition, List<Script>> entry,
                           Map.Entry<ScriptPosition, List<Script>> entry1) {
            ScriptPosition position = entry.getKey();
            ScriptPosition position1 = entry1.getKey();

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
        }
    }

    private static class ScriptCollector implements Collector<Map.Entry<ScriptPosition, List<Script>>, List<BaseComponent[]>, List<BaseComponent[]>> {
        private final Script filter;

        public ScriptCollector(Script filter) {
            this.filter = filter;
        }

        @Override
        public Supplier<List<BaseComponent[]>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<BaseComponent[]>, Map.Entry<ScriptPosition, List<Script>>> accumulator() {
            return (messages, entry) -> {
                ScriptPosition position = entry.getKey();
                List<Script> scripts = entry.getValue();

                for (Script script : scripts) {
                    if (filter != null && isFilterable(script, filter)) {
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
            };
        }

        @Override
        public BinaryOperator<List<BaseComponent[]>> combiner() {
            return (messages, messages1) -> {
                List<BaseComponent[]> result = new ArrayList<>();
                result.addAll(messages);
                result.addAll(messages1);
                return result;
            };
        }

        @Override
        public Function<List<BaseComponent[]>, List<BaseComponent[]>> finisher() {
            return message -> message;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
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

            ImmutableListMultimap<String, String> scriptMap = script.getScript();
            for (String key : scriptMap.keySet()) {
                ImmutableList<String> scriptValues = scriptMap.get(key);
                ImmutableListMultimap<String, String> filterMap = filter.getScript();
                ImmutableList<String> filterValues = filterMap.get(key);

                if (isFilterable(scriptValues, filterValues)) {
                    return true;
                }
            }
            return false;
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
    }
}
