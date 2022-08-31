package xiaoyi311.smoker.smokerplugin.Mods;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import xiaoyi311.smoker.smokerplugin.PluginMain;

/*
PAPI 支持
 */
public class MyPAPI extends PlaceholderExpansion {

    //作者作者
    @Override
    public @NotNull String getAuthor() {
        return "Xiaoyi311";
    }

    //榜以名
    @Override
    public @NotNull String getIdentifier() {
        return "SmokerPlugin";
    }

    //本是本
    @Override
    public @NotNull String getVersion() {
        return "1.4.0";
    }

    //保启用
    @Override
    public boolean persist() {
        return true;
    }

    //得参数请时
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String[] Params = params.split("_");
        if (Params.length != 2){
            return null;
        }
        switch (Params[0]){
            case "jail":
                return PluginMain.INSTANCE.ModJail.ReqPAPI(player, Params[1]);
            default:
                return null;
        }
    }
}