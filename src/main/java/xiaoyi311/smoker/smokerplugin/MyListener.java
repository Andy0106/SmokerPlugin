package xiaoyi311.smoker.smokerplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xiaoyi311.smoker.smokerplugin.Mods.MyJail;

/*
SP 监听系统
 */
public class MyListener implements Listener {
    //命发玩家
    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e){
        if (!MyJail.IsCanUseCommands(e.getPlayer(), e.getMessage())){
            e.setCancelled(true);
        }
    }

    //玩家服务器内
    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        MyJail.PlayerJoin(e.getPlayer());
    }

    //玩家服务器退
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent e){
        MyJail.PlayerQuit(e.getPlayer());
    }

    //玩家移
    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e){
        MyJail.OutJailCheck(e.getPlayer());
    }
}
