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
import java.util.concurrent.atomic.AtomicBoolean;

/*
SP 监狱

MongoDB支持

 */
public class MyJail {
    //未开
    private static boolean State = false;

    //量非在线不久
    private static boolean isCountOffline = true;

    //狱表列表
    private static ConcurrentHashMap<String, Jail> JailList = new ConcurrentHashMap<>();

    //狱中外表
    private static ConcurrentHashMap<UUID, JailPlayer> JailPlayerList = new ConcurrentHashMap<>();

    //有白名
    private static List<String> CommandWhiteList = new ArrayList<>();

    //优家临时立成
    private final Map<Player, Jail> TempJail = new HashMap<>();

    //允实化
    public MyJail(long timerTick, boolean isCountOffline, boolean showBossBar, List<String> commandWhiteList) {
        //愿开之者启否
        if (PluginMain.INSTANCE.config.getBoolean("mods.Jail.enabled", true)){
            try{
                //清空清空
                JailPlayerList = new ConcurrentHashMap<>();
                JailList = new ConcurrentHashMap<>();

                //取狱数
                JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
                JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");
                List<JSONObject> Records = (List<JSONObject>) MyUtils.IsJsonObjectListRead(JailData, "Records");

                //读书取立
                Jails.forEach((k, v) -> {
                    JSONObject jb = (JSONObject) v;
                    boolean isAllWorld = Boolean.parseBoolean(jb.get("IsAllWorld").toString());
                    Jail temp = new Jail();
                    temp.Name = k.toString();
                    temp.IsAllWorld = isAllWorld;
                    temp.Pos1 = isAllWorld ? null : Location.deserialize((Map<String, Object>) jb.get("Pos1"));
                    temp.Pos2 = isAllWorld ? null : Location.deserialize((Map<String, Object>) jb.get("Pos2"));
                    temp.JoinPos = Location.deserialize((Map<String, Object>) jb.get("JoinPos"));
                    temp.OutPos = Location.deserialize((Map<String, Object>) jb.get("OutPos"));
                    temp.WorldName = isAllWorld ? jb.get("WorldName").toString() : null;
                    temp.countOfflineTime = isCountOffline;
                    temp.showBossBar = showBossBar;
                    MyJail.JailList.put(temp.Name, temp);
                });

                Records.forEach((recordJson) -> {
                    JailRecord record = new JailRecord(recordJson);
                    JailPlayerList.put(record.UUID, new JailPlayer(JailList.get(recordJson.get("Jail")), record));
                });
            }catch (Exception e){
                PluginMain.INSTANCE.logger.severe("监狱数据读取失败，请联系管理员！");
                e.printStackTrace();
            }

            //置身以处之
            new JailTimer().start(timerTick, 0);
            new JailTimer().start(timerTick, 1);
            MyJail.isCountOffline = isCountOffline;
            CommandWhiteList = commandWhiteList;

            State = true;
        }
    }

    //取期
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

    //发送狱题
    private static void PriSendJailTitle(Player player) {
        JailPlayer jp = JailPlayerList.get(player.getUniqueId());
        String text = ChatColor.translateAlternateColorCodes('&',
                PluginMain.INSTANCE.config.getString("lang.Jail.ByeToPlayer", "")
                        .replace("{Time}", GetDateMessage(jp.record.Time)));
        player.sendTitle(text, text, 10, 70, 20
        );
    }

    //令可乎
    public static boolean IsCanUseCommands(Player player, String command){
        //当启用否
        if(State){
            //可为狱中顾宗族乎
            if (JailPlayerList.containsKey(player.getUniqueId())){
                //命者不在白名
                if (!CommandWhiteList.contains(command.split(" ")[0])){
                    PriSendJailTitle(player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                    + PluginMain.INSTANCE.config.getString("lang.Jail.CanNotSendCommand", "")
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    //环以测之
    public static void OutJailCheck(Player player) {
        //可为狱中顾宗族乎
        if (JailPlayerList.containsKey(player.getUniqueId())){
            JailPlayer jp = JailPlayerList.get(player.getUniqueId());
            Jail jail = jp.getJail();

            //下狱验
            if (jail.IsAllWorld ?
                    !player.getWorld().getName().equals(jail.WorldName) :
                    !MyUtils.IsInRegion(jail.Pos1, jail.Pos2, player.getLocation().add(0, 1, 0))
            ){
                player.teleport(jail.JoinPos);
                PriSendJailTitle(player);
            }
        }
    }

    //一儿家追录
    public static void PlayerJoin(Player player) {
        //当启用否
        if(State){
            //可为狱中顾宗族乎
            if (JailPlayerList.containsKey(player.getUniqueId())){
                PriSendJailTitle(player);
            }
        }
    }

    //外退
    public static void PlayerQuit(Player player) {
        //当启用否
        if(State){
            //可为狱中顾宗族乎
            if (JailPlayerList.containsKey(player.getUniqueId())){
                //不算不在线长
                if (isCountOffline){
                    JailPlayer jp = JailPlayerList.get(player.getUniqueId());
                    JailRecord record = jp.record;
                    record.LastOnline = false;
                    jp.getJail().addJailRecord(record);
                }
            }
        }
    }

    //命处分
    public void onCommand(CommandSender sender, String[] args) {
        //愿开之者启否
        if (!State){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                            + PluginMain.INSTANCE.config.getString("lang.Game.NotOpen", "")
            ));
            return;
        }

        //数不足
        if (args.length == 1){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                            + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
            ));
        } else {
             switch (args[1]){
                 //佐表列表
                 case "help":
                     sender.sendMessage(GetHelp());
                     break;

                 //狱表列表
                 case "list":
                     //三者当否
                     if (args.length == 2){
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.JailList", "")
                                         .replace("{JailList}", StringUtils.join(JailList.keySet(), ","))
                         ));
                     } else if (args.length == 3) {
                         //断狱在否
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

                 //设世堂堂之世
                 case "setworld":
                     //岂为玩家
                     if (sender instanceof Player player){
                         //盖立成
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设世堂堂之世
                         temp.IsAllWorld = true;
                         temp.WorldName = player.getWorld().getName();
                         TempJail.put(player, temp);
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Jail.SetWorld")
                         ));
                     }else{
                         sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                 PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                         + PluginMain.INSTANCE.config.getString("lang.Game.SenderError", "")
                         ));
                     }
                     break;

                 //设点一
                 case "setpos1":
                     //岂为玩家
                     if (sender instanceof Player player){
                         //盖立成
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设标坐
                         temp.IsAllWorld = false;
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

                 //设点二
                 case "setpos2":
                     //岂为玩家
                     if (sender instanceof Player player){
                         //盖立成
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //设标坐
                         temp.IsAllWorld = false;
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

                 //设下狱点悬于狱
                 case "joinpos":
                     //岂为玩家
                     if (sender instanceof Player player){
                         //盖立成
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

                 //设出狱点
                 case "outpos":
                     //岂为玩家
                     if (sender instanceof Player player){
                         //盖立成
                         Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                         //置点二坐标
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

                 //建狱牢具
                 case "create":
                     //固止于权限乎
                     if (sender.hasPermission("smokerplugin.jail.create")){
                         //岂为玩家
                         if (sender instanceof Player player){
                             //盖立成
                             Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                             //设内、出入狱点
                             if ((temp.IsAllWorld ? temp.WorldName != null : (temp.Pos1 != null && temp.Pos2 != null)) && temp.JoinPos != null && temp.OutPos != null){
                                 //三者当否
                                 if (args.length == 3){
                                     //狱具在狱中者
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

                 //加饮鸠而享刑
                 case "bye":
                     //固止于权限乎
                     if (sender.hasPermission("smokerplugin.jail.bye")){
                         //三者当否
                         if ((args.length == 6 || args.length == 7) && MyUtils.IsInt(args[4])){
                             //狱具在狱中者
                             Jail jail = JailList.get(args[2]);
                             if (jail != null){
                                 //备豫数
                                 String reason = "";
                                 if (args.length == 7){
                                     reason = args[6];
                                 }

                                 //玩家之在否
                                 OfflinePlayer JailPlayer = Bukkit.getOfflinePlayerIfCached(args[3]);
                                 if (JailPlayer != null){
                                     //何意识
                                     if(!JailPlayer.isOp()){
                                         //顾在他坐饮刑狱
                                         JailPlayer player1 = JailPlayerList.get(JailPlayer.getUniqueId());
                                         if (player1 == null || player1.getJail() == jail){
                                             Integer second = Integer.parseInt(args[4]);
                                             //命日式
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

                                             //日体原是
                                             if (second != null){
                                                 JailRecord record = new JailRecord(JailPlayer.getUniqueId(), second, reason, jail);
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

                 //释服而除刑
                 case "back":
                     //固止于权限乎
                     if (sender.hasPermission("smokerplugin.jail.back")){
                         //三者当否
                         if (args.length == 3){
                             //玩家之在否
                             OfflinePlayer JailPlayer = Bukkit.getOfflinePlayerIfCached(args[2]);
                             if (JailPlayer != null){
                                 //一夕被禁锢③，谁为都禁
                                 if(JailPlayerList.containsKey(JailPlayer.getUniqueId())){
                                     JailPlayer jp = JailPlayerList.get(JailPlayer.getUniqueId());
                                     jp.record.back();
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

                 //重设狱位信息
                 case "reset":
                     //固止于权限乎
                     if (sender.hasPermission("smokerplugin.jail.reset")){
                         //岂为玩家
                         if (sender instanceof Player player){
                             //盖立成
                             Jail temp = TempJail.containsKey((Player) sender) ? TempJail.get(player) : new Jail();

                             //设内、出入狱点
                             if (temp.Pos1 != null && temp.Pos2 != null && temp.JoinPos != null && temp.OutPos != null){
                                 //三者当否
                                 if (args.length == 3){
                                     //狱具在狱中者
                                     if (JailList.containsKey(args[2])){
                                         Jail nowJail = JailList.get(args[2]);
                                         AtomicBoolean isJailing = new AtomicBoolean(false);
                                         JailPlayerList.values().forEach((jailPlayer) -> {
                                             if (nowJail == jailPlayer.getJail()){
                                                 isJailing.set(true);
                                             }
                                         });
                                         //岂有做家饮刑措
                                         if (!isJailing.get()){
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

                 //除狱
                 case "delete":
                     //固止于权限乎
                     if (sender.hasPermission("smokerplugin.jail.delete")){
                         //三者当否
                         if (args.length == 3){
                             //狱具在狱中者
                             if (JailList.containsKey(args[2])){
                                 Jail jail = JailList.get(args[2]);
                                 AtomicBoolean isJailing = new AtomicBoolean(false);
                                 JailPlayerList.values().forEach((jailPlayer) -> {
                                     if (jail == jailPlayer.getJail()){
                                         isJailing.set(true);
                                     }
                                 });
                                 //岂有做家饮刑措
                                 if (!isJailing.get()){
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

                 //无此令
                 default:
                     sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                             PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                     + PluginMain.INSTANCE.config.getString("lang.Game.CommandError", "")
                     ));
                     break;
             }
        }
    }

    //取佐表
    private String GetHelp() {
        return
                """
                        ---------------- SmokerPlugin-Jail ----------------
                        /sp jail help -> 显示监狱系统帮助
                        /sp jail list [JailName] -> 获取监狱列表/监狱内服刑玩家列表
                        /sp jail setpos1 -> 将脚下的位置设置点一（与 setworld 谁最后使用则代表什么模式）
                        /sp jail setpos2 -> 将脚下的位置设置点二（与 setworld 谁最后使用则代表什么模式）
                        /sp jail setworld -> 将目前的世界设为监狱世界（与 setpos 谁最后使用则代表什么模式）
                        /sp jail joinpos -> 将脚下的位置设置入狱点
                        /sp jail outpos -> 将脚下的位置设置出狱点
                        /so jail reset <JailName> -> 重新设置监狱的坐标信息
                        /sp jail create <JailName> -> 创建一个监狱
                        /sp jail delete <JailName> -> 删除一个监狱
                        /sp jail back <PlayerName> -> 释放一位玩家
                        /sp jail bye <JailName> <PlayerName> <Time> <d/h/m/s> [Why] -> 让一位玩家服刑
                        ---------------- SmokerPlugin-Jail ----------------
                """;
    }

    //命完补
    public List<String> onTabComplete(String[] args) {
        List<String> commands = new ArrayList<>();
        switch (args.length){
            case 2:
                commands.add("help");
                commands.add("list");
                commands.add("setworld");
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

    //请 PAPI
    public String ReqPAPI(OfflinePlayer player, String param) {
        switch (param){
            //吾监禁日
            case "jailTimeMe":
                //一夕被禁锢③，谁为都禁
                if (JailPlayerList.containsKey(player.getUniqueId())){
                    return GetDateMessage(JailPlayerList.get(player.getUniqueId()).record.Time);
                }
                return "未被监禁";

            //吾监禁故
            case "jailReasonMe":
                //一夕被禁锢③，谁为都禁
                if (JailPlayerList.containsKey(player.getUniqueId())){
                    return JailPlayerList.get(player.getUniqueId()).record.Reason;
                }
                return "未被监禁";

            //未知未知
            default:
                return null;
        }
    }

    //存
    public void save() {
        JailPlayerList.values().forEach((player) -> {
            player.record.saveJSON();
        });
    }

    //存其所有 JSON
    private static void saveJSONAll(JSONObject jailData) {
        PluginMain.INSTANCE.data.put("Jail", jailData);
        try {
            FileWriter fw = new FileWriter(new File(PluginMain.INSTANCE.getDataFolder(), "data.json"));
            fw.write(PluginMain.INSTANCE.data.toJSONString());
            fw.close();
        } catch (IOException e) {
            PluginMain.INSTANCE.logger.severe("写入数据文件失败，请尝试删除data.json");
        }
    }

    //狱具
    private static class Jail{
        //计不在线
        public boolean countOfflineTime = true;

        //以示 BOOS 栏
        public boolean showBossBar = true;

        //系狱名
        public String Name;

        //狱坐标1
        public Location Pos1;

        //狱坐标2
        public Location Pos2;

        //下狱标坐
        public Location JoinPos;

        //狱具标坐
        public Location OutPos;

        //天下皆狱具
        public boolean IsAllWorld = false;

        //世名
        public String WorldName;

        //除狱
        public void delete(){
            //取狱数
            JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
            JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");

            Jails.remove(Name);

            SaveJson(JailData, Jails);
        }

        //存 JSON
        private void SaveJson(JSONObject jailData, JSONObject jails) {
            jailData.put("Jails", jails);
            saveJSONAll(jailData);
        }

        //一新 JSON
        public void updateJSON(){
            //取狱数
            JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");
            JSONObject Jails = (JSONObject) MyUtils.IsJsonObjectRead(JailData, "Jails");
            JSONObject Jail = (JSONObject) MyUtils.IsJsonObjectRead(Jails, Name);

            //其书入立数也
            Jail.put("IsAllWorld", IsAllWorld);
            Jail.put("WorldName", IsAllWorld ? WorldName : null);
            Jail.put("Pos1", IsAllWorld ? null : Pos1.serialize());
            Jail.put("Pos2", IsAllWorld ? null : Pos2.serialize());
            Jail.put("JoinPos", JoinPos.serialize());
            Jail.put("OutPos", OutPos.serialize());
            Jails.put(Name, Jail);

            SaveJson(JailData, Jails);
        }

        //取狱牢者
        public String getJailPlayers() {
            StringBuilder sb = new StringBuilder();
            JailPlayerList.values().forEach((player) -> {
                if (player.getJail() == this){
                    JailRecord record = player.record;
                    sb.append("玩家名：").append(Bukkit.getOfflinePlayer(record.UUID).getName()).append("  释放时间：").append(GetDateMessage(record.Time)).append("  原因：").append(record.Reason).append("\n");
                }
            });
            return sb.toString();
        }

        //加狱饮刑书
        public void addJailRecord(JailRecord record){
            if (JailPlayerList.containsKey(record.UUID)){
                JailPlayer player = JailPlayerList.get(record.UUID);
                JailRecord nowRecord = player.record;
                nowRecord.Time = record.Time;
                JailPlayerList.put(record.UUID, new JailPlayer(player.getJail(), nowRecord));
            }else{
                JailPlayerList.put(record.UUID, new JailPlayer(this, record));
            }
            updateJSON();
        }
    }

    //仰刑书
    private static class JailRecord{
        //谢公语胡儿 UUID
        public UUID UUID;

        //饮刑始期
        public Date Start;

        //终刑日有余
        public Date Time;

        //厌刑凡时
        public Date AllTime;

        //前时新
        private Date LastUpdate;

        //前在绵前乎
        private boolean LastOnline = false;

        //狱具
        private String jail;

        //仰刑之所以为刑也
        public String Reason;

        //然其计则不然
        public BossBar CountDown;

        //允实化
        public JailRecord(UUID uuid, int second, String reason, Jail jail){
            UUID = uuid;
            Start = new Date();
            Time = new Date(second * 1000L);
            AllTime = new Date(second * 1000L);
            Reason = reason;
            CountDown = BossBar.bossBar(Component.text("服刑倒计时 " + GetDateMessage(Time)), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            this.jail = jail.Name;
            saveJSON();
        }

        //存 JSON
        private void saveJSON(){
            //取狱数
            JSONObject JailData = (JSONObject) MyUtils.IsJsonObjectRead(PluginMain.INSTANCE.data, "Jail");

            List<JSONObject> objs = new ArrayList<>();
            JailPlayerList.values().forEach((player) -> objs.add(player.record.GetJSONObject()));
            JailData.put("Records", objs);
            saveJSONAll(JailData);
        }

        //JB 允实化
        public JailRecord(JSONObject jb){
            UUID = java.util.UUID.fromString(jb.get("UUID").toString());
            Start = new Date((long) jb.get("Start"));
            Time = new Date((long) jb.get("Time"));
            AllTime = new Date((long) jb.get("AllTime"));
            Reason = jb.get("Reason").toString();
            CountDown = BossBar.bossBar(Component.text("服刑倒计时 " + GetDateMessage(Time)), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        }

        //取尸 JB 实例之
        public JSONObject GetJSONObject(){
            JSONObject jb = new JSONObject();
            jb.put("Jail", jail);
            jb.put("UUID", UUID.toString());
            jb.put("Start", Start.getTime());
            jb.put("Time", Time.getTime());
            jb.put("AllTime", AllTime.getTime());
            jb.put("Reason", Reason);
            return jb;
        }

        //久之新，日新之
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

        //外被释
        public void back() {
            Player player = Bukkit.getPlayer(UUID);
            if (player != null){
                player.hideBossBar(CountDown);
            }
        }
    }

    //狱系统时钟
    private static class JailTimer extends BukkitRunnable{
        //一概
        private int mode;

        //时钟生蕤钟
        @Override
        public void run() {
            switch (mode){
                case 0:
                    //清饮鸠而终刑
                    JailPlayerList.values().forEach((player) -> {
                        JailRecord record = player.record;
                        Jail jail = player.getJail();
                        if (record.Time.getTime() <= 0){
                            JailPlayerList.remove(record.UUID);
                            jail.updateJSON();
                            JailList.put(jail.Name, jail);
                            String back = ChatColor.translateAlternateColorCodes('&',
                                    PluginMain.INSTANCE.config.getString("lang.Jail.Prefix", "")
                                            + PluginMain.INSTANCE.config.getString("lang.Jail.PlayerBack", "")
                                            .replace("{PlayerName}", Bukkit.getOfflinePlayer(record.UUID).getName())
                            );
                            Bukkit.getOnlinePlayers().forEach((onlinePlayer) -> {
                                if (onlinePlayer.getUniqueId() == record.UUID){
                                    onlinePlayer.teleport(jail.OutPos);
                                    record.back();
                                }
                                onlinePlayer.sendMessage(back);
                            });
                            PluginMain.INSTANCE.logger.info(back);
                        }
                    });
                    break;

                //其计新而不新，其弊也
                case 1:
                    JailPlayerList.values().forEach((jp) -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(jp.getUUID());
                        if (player.isOnline()){
                            jp.record.updateTime();
                        }
                    });
                    break;
            }
        }

        //发初
        public void start(long time, int mode){
            this.mode = mode;
            this.runTaskTimer(PluginMain.INSTANCE, 0, time);
        }
    }

    //狱顾玩家
    public static class JailPlayer{
        //狱具
        private final String jail;

        //仰刑书
        public final JailRecord record;

        //允实化
        public JailPlayer(Jail jail, JailRecord record){
            this.jail = jail.Name;
            this.record = record;
        }

        //取狱牢者
        public Jail getJail() {
            return JailList.get(jail);
        }

        //取尸 UUID
        public UUID getUUID(){
            return record.UUID;
        }
    }
}
