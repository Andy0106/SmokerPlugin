package xiaoyi311.smoker.smokerplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xiaoyi311.smoker.smokerplugin.Mods.MyJail;
import xiaoyi311.smoker.smokerplugin.Mods.MyPAPI;
import xiaoyi311.smoker.smokerplugin.Mods.MyRobot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/*
SP核心
By Xiaoyi311
 */
public final class PluginMain extends JavaPlugin {

    //实例
    public static PluginMain INSTANCE;

    //用户配置
    public FileConfiguration config;

    //使用数据
    public JSONObject data;

    //QQ机器人
    public MyRobot ModRobot;

    //监狱
    public MyJail ModJail;

    //日志输出
    public Logger logger;

    //依赖检测
    public boolean[] check = {false, false};

    //启动时
    @Override
    public void onEnable() {
        INIT();
        logger.info("SmokerPlugin 插件正在启动");
        logger.info("   _____                 _             _____  _             _            \s");
        logger.info("  / ____|               | |           |  __ \\| |           (_)          \s");
        logger.info(" | (___  _ __ ___   ___ | | _____ _ __| |__) | |_   _  __ _ _ _ __       \s");
        logger.info("  \\___ \\| '_ ` _ \\ / _ \\| |/ / _ \\ '__|  ___/| | | | |/ _` | | '_ \\\s");
        logger.info("  ____) | | | | | | (_) |   <  __/ |  | |    | | |_| | (_| | | | | |       ");
        logger.info(" |_____/|_| |_| |_|\\___/|_|\\_\\___|_|  |_|    |_|\\__,_|\\__, |_|_| |_|  ");
        logger.info("                                                       __/ |             \s");
        logger.info("                                                      |___/              \s");
        logger.info("                     SmokerPlugin v1.5.0 By Xiaoyi311                      ");
        logger.info("———————————————————————————————————————————————————————————————————————————");
        logger.info("[*] 正在初始化 SmokerPlugin，开始检测插件依赖情况！");
        CHECK();
        INIT_MOD();
    }

    //检查依赖
    private void CHECK() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logger.info(ChatColor.GREEN + "[√] 检测到 PAPI！已启用 PAPI 相关功能！");
            new MyPAPI().register();
            check[0] = true;
        } else {
            logger.info(ChatColor.RED + "[×] 未检测到 PAPI！已禁用 PAPI 相关功能！");
            check[0] = false;
        }

        if (Bukkit.getPluginManager().getPlugin("MiraiMC") != null) {
            logger.info(ChatColor.GREEN + "[√] 检测到 MiraiMC！已启用 MiraiMC 相关功能！");
            check[1] = true;
        } else {
            logger.info(ChatColor.RED + "[×] 未检测到 MiraiMC！已禁用 MiraiMC 相关功能！");
            check[1] = false;
        }
    }

    //加载功能
    private void INIT_MOD() {
        ModRobot = new MyRobot(
                config.getLong("mods.Robot.qqNumber", 0),
                config.getLong("mods.Robot.groupNumber", 0)
        );
        ModJail = new MyJail(
                config.getLong("mods.Jail.timerTick", 200),
                config.getBoolean("mods.Jail.countOfflineTime", true),
                config.getBoolean("mods.Jail.showBossBar", true),
                (List<String>) config.getList("mods.Jail.commandWhiteList", new ArrayList<>())
        );
    }

    //初始化
    private void INIT() {
        //基础参数
        INSTANCE = this;
        logger = getLogger();
        saveDefaultConfig();
        config = getConfig();
        try {
            File json = new File(getDataFolder(), "data.json");
            if (!json.exists()){
                json.createNewFile();
                FileWriter fw = new FileWriter(json);
                fw.write("{}");
                fw.close();
            }
            data = (JSONObject) new JSONParser().parse(new FileReader(json));
        } catch (IOException | ParseException e) {
            logger.severe("读取数据文件失败，请尝试删除data.json（数据文件）");
            e.printStackTrace();
        }

        //绑定命令
        PluginCommand command;
        command = getCommand("smokerplugin");
        if (command != null){
            command.setExecutor(new MyCommand());
            command.setTabCompleter(new MyCommand());
        } else {
            logger.warning("绑定命令失败！游戏内命令将无法运行");
        }

        //绑定监听
        getServer().getPluginManager().registerEvents(new MyListener(), this);
    }

    //关闭时
    @Override
    public void onDisable() {
        logger.info("SmokerPlugin 插件已卸载～");
        SAVE();
    }

    //保存
    private void SAVE() {
        ModJail.save();
    }

    //重载插件
    public void ReloadPlugin() {
        reloadConfig();
        INIT_MOD();
    }
}
