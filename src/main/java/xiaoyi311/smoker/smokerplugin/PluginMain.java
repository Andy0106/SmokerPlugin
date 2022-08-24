package xiaoyi311.smoker.smokerplugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xiaoyi311.smoker.smokerplugin.Mods.MyJail;
import xiaoyi311.smoker.smokerplugin.Mods.MyRobot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    //启动时
    @Override
    public void onEnable() {
        INIT();
        logger.info("SmokerPlugin 插件已启动～");
        INIT_MOD();
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
                config.getBoolean("mods.Jail.showBossBar", true)
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
    }

    //重载插件
    public void ReloadPlugin() {
        reloadConfig();
        INIT_MOD();
    }
}
