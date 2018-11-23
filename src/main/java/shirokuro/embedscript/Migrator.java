package shirokuro.embedscript;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;
import shirokuro.embedscript.script.EventType;
import shirokuro.embedscript.script.Script;
import shirokuro.embedscript.script.ScriptBlock;
import shirokuro.embedscript.script.ScriptGenerator;
import shirokuro.embedscript.script.ScriptManager;
import shirokuro.embedscript.util.MojangUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author shirokuro
 */
public class Migrator {
    private final ScriptManager scriptManager;
    private final CommandSender commandSender;

    public Migrator(CommandSender commandSender, ScriptManager scriptManager, Plugin scriptBlock) {
        this.commandSender = commandSender;
        this.scriptManager = scriptManager;

        Path dataFolder = scriptBlock.getDataFolder().toPath().toAbsolutePath();
        Path scriptBlockBlocksData = Paths.get(dataFolder.getParent().toString(), "ScriptBlock", "BlocksData").toAbsolutePath();
        Path interactScripts = Paths.get(scriptBlockBlocksData.toString(), "interact_Scripts.yml");
        Path walkScripts = Paths.get(scriptBlockBlocksData.toString(), "walk_Scripts.yml");

        Yaml yaml = new Yaml();

        Map<String, Map<String, List<String>>> interactList;
        try (BufferedReader reader = Files.newBufferedReader(interactScripts)) {
            interactList = yaml.load(reader);
        } catch (IOException e) {
            commandSender.sendMessage(Prefix.ERROR_PREFIX + "Failed to load list of interact script from ScriptBlock.");
            throw new RuntimeException("Failed to load list of interact script from ScriptBlock.", e);
        }

        Map<String, Map<String, List<String>>> walkList;
        try (BufferedReader reader = Files.newBufferedReader(walkScripts)) {
            walkList = yaml.load(reader);
        } catch (IOException e) {
            commandSender.sendMessage(Prefix.ERROR_PREFIX + "Failed to load list of walk script from ScriptBlock.");
            throw new RuntimeException("Failed to load list of walk script from ScriptBlock.", e);
        }

        migrate(EventType.INTERACT, interactList);
        migrate(EventType.WALK, walkList);
    }

    private void migrate(EventType type, Map<String, Map<String, List<String>>> map) {
        for (Map.Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
            for (Map.Entry<String, List<String>> listEntry : entry.getValue().entrySet()) {
                String[] coordinate = listEntry.getKey().split(",");

                List<String> strings = listEntry.getValue();
                Script script = ScriptGenerator.generateFromString(Bukkit.getConsoleSender(),
                    MojangUtil.getUUID(strings.get(0).split("/")[0].split(":")[1]), strings.get(1));

                ScriptBlock block = new ScriptBlock(entry.getKey(),
                    Integer.parseInt(coordinate[0]),
                    Integer.parseInt(coordinate[1]),
                    Integer.parseInt(coordinate[2]));

                if (scriptManager.hasScript(type, block)) {
                    scriptManager.add(commandSender, type, block, script);
                } else {
                    scriptManager.embed(commandSender, type, block, script);
                }
            }
        }
    }
}
