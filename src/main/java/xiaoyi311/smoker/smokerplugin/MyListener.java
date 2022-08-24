package xiaoyi311.smoker.smokerplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xiaoyi311.smoker.smokerplugin.Mods.MyJail;

public class MyListener implements Listener {
    //玩家发送命令
    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e){
        if (!MyJail.IsCanUseCommands(e.getPlayer())){
            e.setCancelled(true);
        }
    }

    //玩家加入服务器
    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        MyJail.PlayerJoin(e.getPlayer());
    }

    //玩家退出服务器
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent e){
        MyJail.PlayerQuit(e.getPlayer());
    }

    //玩家移动
    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e){
        MyJail.OutJailCheck(e.getPlayer());
    }
}
