package xiaoyi311.smoker.smokerplugin.Mods;

import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import xiaoyi311.smoker.smokerplugin.PluginMain;

/*
SP QQ机器人
By Xiaoyi311
 */
public class MyRobot {
    //是否启动
    private boolean State = false;

    //群号码
    private long GroupNumber;

    //机器人
    private MiraiBot Bot;

    //实例化
    public MyRobot(long QQNumber, long GroupNumber) {
        if (PluginMain.INSTANCE.config.getBoolean("mods.Robot.enabled", true) && PluginMain.INSTANCE.check[1]){
            if (QQNumber == 0) {
                PluginMain.INSTANCE.logger.warning("警告！QQ号未输入！QQ机器人功能将禁用");
                return;
            }

            Bot = MiraiBot.getBot(QQNumber);
            this.GroupNumber = GroupNumber;
            State = true;
        }
    }

    //发送信息到群
    public void SendMessageToGroup(String message){
        if (State && Bot.isOnline()){
            MiraiGroup group = Bot.getGroup(GroupNumber);
            if (group != null){
                group.sendMessage(message);
            }else {
                PluginMain.INSTANCE.logger.warning("警告！群号错误！");
            }
        }
    }
}
