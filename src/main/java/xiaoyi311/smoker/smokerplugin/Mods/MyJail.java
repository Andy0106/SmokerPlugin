package xiaoyi311.smoker.smokerplugin.Mods;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import xiaoyi311.smoker.smokerplugin.MyUtils;
import xiaoyi311.smoker.smokerplugin.PluginMain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/*
SP 监狱

PAPI支持
MongoDB支持

 */
public class MyJail {
    //是否开启
    private static boolean State = false;

    //是否计算非在线时间
    private static boolean isCountOffline = true;

    //监狱列表
    private static ConcurrentHashMap<String, Jail> JailList = new ConcurrentHashMap<>();

    //监狱玩家列表
    private static Map<UUID, JailPlayer> JailPlayerList = new HashMap<>();

    //玩家临时数据
    private final Map<Player, Jail> TempJail = new HashMap<>();

    //实例化
    public MyJail(long timerTick, boolean isCountOffline, boolean showBossBar) {
        //是否开启
        if (PluginMain.INSTANCE.config.getBoolean("mods.Jail.enabled", true)){
            try{
                //清空
                JailPlayerList = new HashMap<>();
                JailList = new ConcurrentHashMap<>();

                //获取监狱数据
                JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
                JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");

                //读取数据
                Jails.forEach((k, v) -> {
                    JSONObject jb = (JSONObject) v;
                    Jail temp = new Jail();
                    temp.Name = k.toString();
                    temp.Pos1 = Location.deserialize((Map<String, Object>) jb.get("Pos1"));
                    temp.Pos2 = Location.deserialize((Map<String, Object>) jb.get("Pos2"));
                    temp.JoinPos = Location.deserialize((Map<String, Object>) jb.get("JoinPos"));
                    temp.OutPos = Location.deserialize((Map<String, Object>) jb.get("OutPos"));
                    CopyOnWriteArrayList<JailRecord> jrs = new CopyOnWriteArrayList<>();
                    ((List<JSONObject>) jb.get("Records")).forEach((recordJson) -> {
                        JailRecord record = new JailRecord(recordJson);
                        JailPlayerList.put(record.UUID, new JailPlayer(temp, record));
                        jrs.add(record);
                    });
                    temp.JailRecords = jrs;
                    temp.countOfflineTime = isCountOffline;
                    MyJail.JailList.put(temp.Name, temp);
                });
            }catch (Exception e){
                PluginMain.INSTANCE.logger.severe("监狱数据读取失败，请联系管理员！");
                e.printStackTrace();
            }

            //应用配置
            new JailTimer().start(timerTick, 0);
            new JailTimer().start(timerTick, 1);
            MyJail.isCountOffline = isCountOffline;

            State = true;
        }
    }

    //获取日期
    private static String GetDateMessage(Date date){
        StringBuilder sb = new StringBuilder();
        double AllSecond = Math.floor(date.getTime() / 1000);
        int Second = (int) (AllSecond % 60);
        double AllMinute = Math.floor(AllSecond / 60);
        int Minute = (int) AllMinute % 60;
        double AllHour = Math.floor(AllMinute / 60);
        int Hour = (int) (AllHour % 24);
        int Day = (int) Math.floor(AllHour / 24);

        sb.append(Day).append("天 ");
        sb.append(Hour).append("小时 ");
        sb.append(Minute).append("分 ");
        sb.append(Second).append("秒");

        return sb.toString();
    }

    //发送监狱标题
    private static void PriSendJailTitle(Player player) {
        JailPlayer jp = JailPlayerList.get(player.getUniqueId());
        String text = ChatColor.translateAlternateColorCodes('&',
                PluginMain.INSTANCE.config.getString("lang.Jail.ByeToPlayer", "")
                        .replace("{Time}", GetDateMessage(jp.getRecord().Time)));
        player.sendTitle(text, text, 10, 70, 20
        );
    }

    //命令是否允许
    public static boolean IsCanUseCommands(Player player){
        //是否启用
        if(State){
            //是否为监狱玩家
            if (JailPlayerList.containsKey(player.getUniqueId())){
                PriSendJailTitle(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                + PluginMain.INSTANCE.config.getString("lang.Jail.CanNotSendCommand", "")
                ));
                return false;
            }
        }
        return true;
    }

    //越狱检测
    public static void OutJailCheck(Player player) {
        //是否为监狱玩家
        if (JailPlayerList.containsKey(player.getUniqueId())){
            JailPlayer jp = JailPlayerList.get(player.getUniqueId());
            Jail jail = jp.getJail();
            //是否离开监狱范围
            if (!MyUtils.IsInRegion(jail.Pos1, jail.Pos2, player.getLocation().add(0, 1, 0))){
                player.teleport(jail.JoinPos);
                PriSendJailTitle(player);
            }
        }
    }

    public static void PlayerJoin(Player player) {
        //是否启用
        if(State){
            //是否为监狱玩家
            if (JailPlayerList.containsKey(player.getUniqueId())){
                PriSendJailTitle(player);
            }
        }
    }

    //玩家退出
    public static void PlayerQuit(Player player) {
        //是否启用
        if(State){
            //是否为监狱玩家
            if (JailPlayerList.containsKey(player.getUniqueId())){
                //是否不计算不在线时长
                if (isCountOffline){
                    JailPlayer jp = JailPlayerList.get(player.getUniqueId());
                    JailRecord record = jp.getRecord();
                    record.LastOnline = false;
                    jp.getJail().addJailRecord(record);
                }
            }
        }
    }

    //命令处理
    public void onCommand(CommandSender sender, String[] args) {
        //是否开启
        if (!State){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                            + PluginMain.INSTANCE.config.getString("lang.Game.NotOpen", "")
            ));
            return;
        }

        //参数是否不足
        if (args.length == 1){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                            + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
            ));
        } else {
             switch (args[1]){
                 //帮助列表
                 case "help":
                     sender.sendMessage(GetHelp());
                     break;

                 //监狱列表
                 case "list":
                     //参数是否正确
                     if (args.length == 2){
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.JailList", "")
                                         .replace("{JailList}", StringUtils.join(JailList.keySet(), ","))
                         ));
                     } else if (args.length == 3) {
                         //判断监狱是否存在
                         Jail jail = JailList.get(args[2]);
                         if (jail != null){
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Jail.JailPlayer", "")
                                             .replace("{JailName}", args[2])
                                             .replace("{JailPlayerList}", jail.getJailPlayers())
                             ));
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Jail.NoJail", "")
                             ));
                         }
                     } else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                         ));
                     }
                     break;

                 //设置点一
                 case "setpos1":
                     //是否为玩家
                     if (sender instanceof Player player){
                         //临时数据
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设置点一坐标
                         temp.Pos2 = player.getLocation();
                         TempJail.put(player, temp);
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.SetPos1")
                         ));
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                         ));
                     }
                     break;

                 //设置点二
                 case "setpos2":
                     //是否为玩家
                     if (sender instanceof Player player){
                         //临时数据
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设置点二坐标
                         temp.Pos1 = player.getLocation();
                         TempJail.put(player, temp);
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.SetPos2")
                         ));
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                         ));
                     }
                     break;

                 //设置入狱点
                 case "joinpos":
                     //是否为玩家
                     if (sender instanceof Player player){
                         //临时数据
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设置点二坐标
                         temp.JoinPos = player.getLocation();
                         TempJail.put(player, temp);
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.SetJoinPos")
                         ));
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                         ));
                     }
                     break;

                 //设置出狱点
                 case "outpos":
                     //是否为玩家
                     if (sender instanceof Player player){
                         //临时数据
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设置点二坐标
                         temp.OutPos = player.getLocation();
                         TempJail.put(player, temp);
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.SetOutPos")
                         ));
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                         ));
                     }
                     break;

                 //创建监狱
                 case "create":
                     //是否存在权限
                     if (sender.hasPermission("smokerplugin.jail.create")){
                         //是否为玩家
                         if (sender instanceof Player player){
                             //临时数据
                             Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                             //是否已设置范围和出入狱点
                             if (temp.Pos1 != null && temp.Pos2 != null && temp.JoinPos != null && temp.OutPos != null){
                                 //参数是否正确
                                 if (args.length == 3){
                                     //监狱是否存在
                                     if (!JailList.containsKey(args[2])){
                                         temp.Name = args[2];
                                         temp.updateJSON();
                                         JailList.put(args[2], temp);
                                         TempJail.remove(player);
                                         String test = ChatColor.translateAlternateColorCodes('&',
                                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                         + PluginMain.INSTANCE.config.getString("lang.Jail.JailCreate", "")
                                                         .replace("{JailName}", args[2])
                                         );
                                         sender.sendMessage(test);
                                         PluginMain.INSTANCE.logger.info(test);
                                     }else{
                                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                         + PluginMain.INSTANCE.config.getString("lang.Jail.JailExists", "")
                                         ));
                                     }
                                 }else{
                                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                                     ));
                                 }
                             }else{
                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                 + PluginMain.INSTANCE.config.getString("lang.Jail.PosNull", "")
                                 ));
                             }
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                             ));
                         }
                     }else {
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.NoPermission", "")
                         ));
                     }
                     break;

                 //添加服刑
                 case "bye":
                     //是否存在权限
                     if (sender.hasPermission("smokerplugin.jail.bye")){
                         //参数是否正确
                         if ((args.length == 6 || args.length == 7) && MyUtils.IsInt(args[4])){
                             //监狱是否存在
                             Jail jail = JailList.get(args[2]);
                             if (jail != null){
                                 //预备参数
                                 String reason = "";
                                 if (args.length == 7){
                                     reason = args[6];
                                 }

                                 //玩家是否存在
                                 OfflinePlayer JailPlayer = Bukkit.getOfflinePlayerIfCached(args[3]);
                                 if (JailPlayer != null){
                                     //玩家是否为op
                                     if(!JailPlayer.isOp()){
                                         //玩家是否在其他监狱服刑
                                         AtomicBoolean isJailing = new AtomicBoolean(false);
                                         JailList.values().forEach((OtherJail) -> OtherJail.JailRecords.forEach((record) -> {
                                             if (record.UUID == JailPlayer.getUniqueId() && OtherJail != jail){
                                                 isJailing.set(true);
                                             }
                                         }));
                                         if (!isJailing.get()){
                                             Integer second = Integer.parseInt(args[4]);
                                             //判断时间格式
                                             switch (args[5]){
                                                 case "d":
                                                     second *= 60*60*12;
                                                     break;
                                                 case "h":
                                                     second *= 60*60;
                                                     break;
                                                 case "m":
                                                     second *= 60;
                                                 case "s":
                                                     break;
                                                 default:
                                                     second = null;
                                                     break;
                                             }

                                             //时间格式是否正确
                                             if (second != null){
                                                 JailRecord record = new JailRecord(JailPlayer.getUniqueId(), second, reason);
                                                 jail.addJailRecord(record);
                                                 JailPlayerList.put(record.UUID, new JailPlayer(jail, record));
                                                 String bye = ChatColor.translateAlternateColorCodes('&',
                                                         PluginMain.INSTANCE.config.getString("lang.Jail.Bye", "")
                                                                 .replace("{Player}", args[3])
                                                                 .replace("{Sender}", sender.getName())
                                                                 .replace("{JailName}", jail.Name)
                                                                 .replace("{Time}", GetDateMessage(record.Time))
                                                 );
                                                 Bukkit.getOnlinePlayers().forEach((player) -> {
                                                     if (player.getUniqueId() == JailPlayer.getUniqueId()){
                                                         player.teleport(jail.JoinPos);
                                                         String text = ChatColor.translateAlternateColorCodes('&',
                                                                 PluginMain.INSTANCE.config.getString("lang.Jail.ByeToPlayer", "")
                                                                         .replace("{Time}", GetDateMessage(record.Time)));
                                                         player.sendTitle(text, text, 10, 70 ,20);
                                                     }
                                                     player.sendMessage(bye);
                                                 });
                                                 PluginMain.INSTANCE.logger.info(bye);
                                             }else{
                                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                                 + PluginMain.INSTANCE.config.getString("lang.Game.TimeTypeError", "")
                                                 ));
                                             }
                                         }else{
                                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                             + PluginMain.INSTANCE.config.getString("lang.Jail.IsJailing", "")
                                                             .replace("{PlayerName}", args[3])
                                             ));
                                         }
                                     } else {
                                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                         + PluginMain.INSTANCE.config.getString("lang.Jail.IsOP", "")
                                         ));
                                     }
                                 }else{
                                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Game.NoPlayer", "")
                                     ));
                                 }
                             }else{
                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                 + PluginMain.INSTANCE.config.getString("lang.Jail.NoJail", "")
                                 ));
                             }
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                             ));
                         }
                     } else {
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.NoPermission", "")
                         ));
                     }
                     break;

                 //解除服刑
                 case "back":
                     //是否存在权限
                     if (sender.hasPermission("smokerplugin.jail.back")){
                         //参数是否正确
                         if (args.length == 3){
                             //玩家是否存在
                             OfflinePlayer JailPlayer = Bukkit.getOfflinePlayerIfCached(args[2]);
                             if (JailPlayer != null){
                                 //玩家是否被监禁
                                 if(JailPlayerList.containsKey(JailPlayer.getUniqueId())){
                                     JailPlayer jp = JailPlayerList.get(JailPlayer.getUniqueId());
                                     jp.getRecord().back();
                                     jp.getJail().JailRecords.remove(jp.getRecord());
                                     jp.getJail().updateJSON();
                                     JailList.put(jp.getJail().Name, jp.getJail());
                                     JailPlayerList.remove(JailPlayer.getUniqueId());
                                     String back = ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Jail.PlayerBack", "")
                                                     .replace("{PlayerName}", args[2])
                                     );
                                     Bukkit.getOnlinePlayers().forEach((player) -> {
                                         if (player.getUniqueId() == JailPlayer.getUniqueId()){
                                             player.teleport(jp.getJail().OutPos);
                                         }
                                         player.sendMessage(back);
                                     });
                                     PluginMain.INSTANCE.logger.info(back);
                                 }else{
                                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Jail.PlayerNoJail", "")
                                                     .replace("{PlayerName}", args[2])
                                     ));
                                 }
                             }else{
                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                 + PluginMain.INSTANCE.config.getString("lang.Game.NoPlayer", "")
                                 ));
                             }
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                             ));
                         }
                     } else {
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.NoPermission", "")
                         ));
                     }
                     break;

                 //重设监狱位置信息
                 case "reset":
                     //是否存在权限
                     if (sender.hasPermission("smokerplugin.jail.reset")){
                         //是否为玩家
                         if (sender instanceof Player player){
                             //临时数据
                             Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                             //是否已设置范围和出入狱点
                             if (temp.Pos1 != null && temp.Pos2 != null && temp.JoinPos != null && temp.OutPos != null){
                                 //参数是否正确
                                 if (args.length == 3){
                                     //监狱是否存在
                                     if (JailList.containsKey(args[2])){
                                         Jail nowJail = JailList.get(args[2]);
                                         //是否有玩家在服刑
                                         if (nowJail.JailRecords.isEmpty()){
                                             nowJail.JoinPos = temp.JoinPos;
                                             nowJail.OutPos = temp.OutPos;
                                             nowJail.Pos1 = temp.Pos1;
                                             nowJail.Pos2 = temp.Pos2;
                                             nowJail.updateJSON();
                                             JailList.put(args[2], nowJail);
                                             TempJail.remove(player);
                                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                             + PluginMain.INSTANCE.config.getString("lang.Jail.JailReset", "")
                                                             .replace("{JailName}", args[2])
                                             ));
                                         }else{
                                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                             + PluginMain.INSTANCE.config.getString("lang.Jail.CanNotChange", "")
                                             ));
                                         }
                                     }else{
                                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                         + PluginMain.INSTANCE.config.getString("lang.Jail.NoJail", "")
                                         ));
                                     }
                                 }else{
                                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                                     ));
                                 }
                             }else{
                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                 + PluginMain.INSTANCE.config.getString("lang.Jail.PosNull", "")
                                 ));
                             }
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                             ));
                         }
                     }else {
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.NoPermission", "")
                         ));
                     }
                     break;

                 //删除监狱
                 case "delete":
                     //是否存在权限
                     if (sender.hasPermission("smokerplugin.jail.delete")){
                         //参数是否正确
                         if (args.length == 3){
                             //监狱是否存在
                             if (JailList.containsKey(args[2])){
                                 Jail jail = JailList.get(args[2]);
                                 //是否有玩家在服刑
                                 if (jail.JailRecords.isEmpty()){
                                     jail.delete();
                                     JailList.remove(jail.Name);
                                     String test = ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Jail.JailDelete", "")
                                                     .replace("{JailName}", jail.Name)
                                     );
                                     sender.sendMessage(test);
                                     PluginMain.INSTANCE.logger.info(test);
                                 }else{
                                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                     + PluginMain.INSTANCE.config.getString("lang.Jail.CanNotChange", "")
                                     ));
                                 }
                             }else{
                                 sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                         PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                                 + PluginMain.INSTANCE.config.getString("lang.Jail.NoJail", "")
                                 ));
                             }
                         }else{
                             sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                     PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                             + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                             ));
                         }
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.NoPermission", "")
                         ));
                     }
                     break;

                 //无此命令
                 default:
                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                     + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                     ));
                     break;
             }
        }
    }

    //获取帮助列表
    private String GetHelp() {
        return
                """
                        ---------------- SmokerPlugin-Jail ----------------
                        /sp jail help -> 显示监狱系统帮助
                        /sp jail list [JailName] -> 获取监狱列表/监狱内服刑玩家列表
                        /sp jail setpos1 -> 将脚下的位置设置点一
                        /sp jail setpos2 -> 将脚下的位置设置点二
                        /sp jail joinpos -> 将脚下的位置设置入狱点
                        /sp jail outpos -> 将脚下的位置设置出狱点
                        /so jail reset <JailName> -> 重新设置监狱的坐标信息
                        /sp jail create <JailName> -> 创建一个监狱
                        /sp jail delete <JailName> -> 删除一个监狱
                        /sp jail back <PlayerName> -> 释放一位玩家
                        /sp jail bye <JailName> <PlayerName> <Time> <d/h/m/s> [Why] -> 让一位玩家服刑
                        ---------------- SmokerPlugin-Jail ----------------""";
    }

    //命令补全
    public List<String> onTabComplete(String[] args) {
        List<String> commands = new ArrayList<>();
        switch (args.length){
            case 2:
                commands.add("help");
                commands.add("list");
                commands.add("setpos1");
                commands.add("setpos2");
                commands.add("joinpos");
                commands.add("outpos");
                commands.add("reset");
                commands.add("create");
                commands.add("delete");
                commands.add("back");
                commands.add("bye");
                return commands;
            case 3:
                switch (args[1]){
                    case "list", "bye":
                        commands.addAll(JailList.keySet());
                        return commands;
                }
                break;
            case 6:
                switch (args[1]){
                    case "bye", "delete", "reset":
                        commands.add("d");
                        commands.add("m");
                        commands.add("s");
                        return commands;
                }
                break;
        }
        return null;
    }

    //监狱
    private static class Jail{
        //计算不在线时间
        public boolean countOfflineTime = true;

        //展示Boss栏
        public boolean showBossBar = true;

        //监狱名称
        public String Name;

        //监狱坐标1
        public Location Pos1;

        //监狱坐标2
        public Location Pos2;

        //入狱坐标
        public Location JoinPos;

        //出狱坐标
        public Location OutPos;

        //服刑记录
        private CopyOnWriteArrayList<JailRecord> JailRecords = new CopyOnWriteArrayList<>();

        //删除监狱
        public void delete(){
            //获取监狱数据
            JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
            JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");

            Jails.remove(Name);

            SaveJson(JailData, Jails);
        }

        //保存JSON
        private void SaveJson(JSONObject jailData, JSONObject jails) {
            jailData.put("Jails", jails);
            PluginMain.INSTANCE.data.put("Jail", jailData);
            try {
                FileWriter fw = new FileWriter(new File(PluginMain.INSTANCE.getDataFolder(), "data.json"));
                fw.write(PluginMain.INSTANCE.data.toJSONString());
                fw.close();
            } catch (IOException e) {
                PluginMain.INSTANCE.logger.severe("写入数据文件失败，请尝试删除data.json");
            }
        }

        //更新JSON
        public void updateJSON(){
            //获取监狱数据
            JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
            JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");
            JSONObject Jail = (JSONObject) MyUtils.IsJsonObjectRead(Jails, Name);

            //写入数据
            Jail.put("Pos1", Pos1.serialize());
            Jail.put("Pos2", Pos2.serialize());
            Jail.put("JoinPos", JoinPos.serialize());
            Jail.put("OutPos", OutPos.serialize());
            List<JSONObject> Records = new ArrayList<>();
            JailRecords.forEach((record) -> {
                Records.add(record.GetJSONObject());
            });
            Jail.put("Records", Records);
            Jails.put(Name, Jail);

            SaveJson(JailData, Jails);
        }

        //获取监狱人员
        public String getJailPlayers() {
            StringBuilder sb = new StringBuilder();
            JailRecords.forEach((record) -> {
                sb.append("玩家名：").append(Bukkit.getOfflinePlayer(record.UUID).getName()).append("  释放时间：").append(GetDateMessage(record.Time)).append("  原因：").append(record.Reason).append("\n");
            });
            return sb.toString();
        }

        //添加监狱服刑记录
        public void addJailRecord(JailRecord record){
            AtomicBoolean isHave = new AtomicBoolean(false);
            JailRecords.forEach(((serverRecord) -> {
                if (serverRecord.UUID == record.UUID){
                    serverRecord.Time = record.Time;
                    isHave.set(true);
                }
            }));
            if (!isHave.get()){
                JailRecords.add(record);
                JailPlayerList.put(record.UUID, new JailPlayer(this, record));
            }
            updateJSON();
        }
    }

    //服刑记录
    private static class JailRecord{
        //玩家UUID
        public UUID UUID;

        //服刑开始时间
        public Date Start;

        //服刑剩余时间
        public Date Time;

        //服刑总计时间
        public Date AllTime;

        //上次更新时间
        private Date LastUpdate;

        //上次是否在线
        private boolean LastOnline = false;

        //服刑原因
        public String Reason;

        //倒计时
        public BossBar CountDown;

        //实例化
        public JailRecord(UUID uuid, int second, String reason){
            UUID = uuid;
            Start = new Date();
            Time = new Date(second * 1000L);
            AllTime = new Date(second * 1000L);
            Reason = reason;
            CountDown = BossBar.bossBar(Component.text("服刑倒计时 " + GetDateMessage(Time)), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        }

        //JB实例化
        public JailRecord(JSONObject jb){
            UUID = java.util.UUID.fromString(jb.get("UUID").toString());
            Start = new Date((long) jb.get("Start"));
            Time = new Date((long) jb.get("Time"));
            AllTime = new Date((long) jb.get("AllTime"));
            Reason = jb.get("Reason").toString();
            CountDown = BossBar.bossBar(Component.text("服刑倒计时 " + GetDateMessage(Time)), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        }

        //获取JB实例
        public JSONObject GetJSONObject(){
            JSONObject jb = new JSONObject();
            jb.put("UUID", UUID.toString());
            jb.put("Start", Start.getTime());
            jb.put("Time", Time.getTime());
            jb.put("AllTime", AllTime.getTime());
            jb.put("Reason", Reason);
            return jb;
        }

        //更新时间刻
        public void updateTime() {
            if (!LastOnline){
                LastOnline = true;
                LastUpdate = new Date();
                return;
            }
            Date date = Time;
            long tick = new Date().getTime() - LastUpdate.getTime();
            Time = new Date(date.getTime() - tick);
            LastUpdate = new Date();
            Player player = Bukkit.getPlayer(UUID);
            if (player != null && JailPlayerList.get(UUID).getJail().showBossBar){
                player.hideBossBar(CountDown);
                float progress = (float) (AllTime.getTime() - Time.getTime()) / AllTime.getTime();
                if (progress <= 1){
                    CountDown = BossBar.bossBar(
                            Component.text("服刑倒计时 " + GetDateMessage(Time)),
                            (float) (AllTime.getTime() - Time.getTime()) / AllTime.getTime(),
                            BossBar.Color.RED,
                            BossBar.Overlay.PROGRESS
                    );
                }
                player.showBossBar(CountDown);
            }
        }

        //玩家被释放
        public void back() {
            Player player = Bukkit.getPlayer(UUID);
            if (player != null){
                player.hideBossBar(CountDown);
            }
        }
    }

    //监狱系统时间钟
    private static class JailTimer extends BukkitRunnable{
        //模式
        private int mode;

        //时间钟
        @Override
        public void run() {
            switch (mode){
                case 0:
                    //清理服刑完成
                    JailList.values().forEach((jail) -> jail.JailRecords.forEach((record) -> {
                        if (record.Time.getTime() <= 0){
                            JailPlayerList.remove(record.UUID);
                            jail.JailRecords.remove(record);
                            jail.updateJSON();
                            JailList.put(jail.Name, jail);
                            String back = ChatColor.translateAlternateColorCodes('&',
                                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                            + PluginMain.INSTANCE.config.getString("lang.Jail.PlayerBack", "")
                                            .replace("{PlayerName}", Bukkit.getOfflinePlayer(record.UUID).getName())
                            );
                            Bukkit.getOnlinePlayers().forEach((player) -> {
                                if (player.getUniqueId() == record.UUID){
                                    player.teleport(jail.OutPos);
                                    record.back();
                                }
                                player.sendMessage(back);
                            });
                            PluginMain.INSTANCE.logger.info(back);
                        }
                    }));
                    break;

                //计时更新
                case 1:
                    JailPlayerList.values().forEach((jp) -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(jp.getUUID());
                        if (player.isOnline()){
                            jp.getRecord().updateTime();
                        }
                    });
                    break;
            }
        }

        //启动
        public void start(long time, int mode){
            this.mode = mode;
            this.runTaskTimer(PluginMain.INSTANCE, 0, time);
        }
    }

    //监狱玩家
    public static class JailPlayer{
        private final String jail;
        private final UUID record;

        //实例化
        public JailPlayer(Jail jail, JailRecord record){
            this.jail = jail.Name;
            this.record = record.UUID;
        }

        //获取监狱
        public Jail getJail() {
            return JailList.get(jail);
        }

        //获取UUID
        public UUID getUUID(){
            return record;
        }

        //获取服刑记录
        public JailRecord getRecord(){
            AtomicReference<JailRecord> result = new AtomicReference<>(null);
            getJail().JailRecords.forEach((record) -> {
                if (record.UUID == this.record){
                    result.set(record);
                }
            });
            return result.get();
        }
    }
}
