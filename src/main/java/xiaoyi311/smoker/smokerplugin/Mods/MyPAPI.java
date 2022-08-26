package xiaoyi311.smoker.smokerplugin.Mods;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import xiaoyi311.smoker.smokerplugin.PluginMain;

public class MyPAPI extends PlaceholderExpansion {

    //作者
    @Override
    public @NotNull String getAuthor() {
        return "Xiaoyi311";
    }

    //插件名
    @Override
    public @NotNull String getIdentifier() {
        return "SmokerPlugin";
    }

    //版本
    @Override
    public @NotNull String getVersion() {
        return "1.4.0";
    }

    //保持启用
    @Override
    public boolean persist() {
        return true;
    }

    //收到参数请求时
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String[] Params = params.split("_");
        switch (Params[1]){
            case "jail":
                return PluginMain.INSTANCE.ModJail.ReqPAPI(player, Params[2]);
            default:
                return null;
        }
    }
}