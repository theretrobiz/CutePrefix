package retrobiz.cutePrefix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.md_5.bungee.api.ChatColor;

import java.util.*;

public class CutePrefix extends JavaPlugin implements Listener {

    private List<String> emotions;
    private Map<String, List<String>> gradients;
    private Map<UUID, String> playerEmotion = new HashMap<>();
    private Random random = new Random();
    private boolean autoChange;
    private int intervalSeconds;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            setRandomEmotion(player);
            setupTeam(player);
        }

        if (autoChange) {
            startEmotionChanger();
        }

        getLogger().info("CutePrefix loaded!");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();

        emotions = config.getStringList("emotions.list");

        gradients = new HashMap<>();
        for (String emotion : emotions) {
            List<String> gradient = config.getStringList("emotions.gradients." + emotion);
            gradients.put(emotion, gradient);
        }

        autoChange = config.getBoolean("auto-change.enabled");
        intervalSeconds = config.getInt("auto-change.interval-seconds");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setRandomEmotion(player);
        setupTeam(player);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = getTeam(player);

        if (team != null) {
            event.setFormat(team.getPrefix() + "§r%1$s: %2$s");
        }
    }

    private void setRandomEmotion(Player player) {
        if (emotions.isEmpty()) return;
        String emotion = emotions.get(random.nextInt(emotions.size()));
        playerEmotion.put(player.getUniqueId(), emotion);
    }

    private void setupTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "prefix_" + player.getName();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.addEntry(player.getName());
        updatePrefix(player, team);
    }

    private Team getTeam(Player player) {
        return Bukkit.getScoreboardManager()
                .getMainScoreboard()
                .getTeam("prefix_" + player.getName());
    }

    private void updatePrefix(Player player, Team team) {

        String emotion = playerEmotion.getOrDefault(player.getUniqueId(), emotions.get(0));

        List<String> gradient = gradients.get(emotion);
        String start = gradient.get(0);
        String end = gradient.get(1);

        String gradientText = applyGradient(emotion, start, end);

        team.setPrefix(gradientText + " ");
        player.setPlayerListName(gradientText + " §r" + player.getName());
    }

    private void startEmotionChanger() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    setRandomEmotion(player);
                    Team team = getTeam(player);
                    if (team != null) {
                        updatePrefix(player, team);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L * intervalSeconds);
    }

    private String applyGradient(String text, String startHex, String endHex) {

        java.awt.Color start = java.awt.Color.decode(startHex);
        java.awt.Color end = java.awt.Color.decode(endHex);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            float ratio = (float) i / (text.length() - 1);

            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            ChatColor color = ChatColor.of(new java.awt.Color(red, green, blue));

            result.append(color).append(text.charAt(i));
        }

        return result.toString();
    }
}