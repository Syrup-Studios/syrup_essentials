package net.syrupstudios.syrupessentials.data;

import com.mojang.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.syrupstudios.syruplibrary.teleport.TeleportTarget;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;


@Data
@AllArgsConstructor
public abstract class Locations {
    private static final Logger LOGGER = LogUtils.getLogger();
    private HashMap<String, TeleportTarget> destinations;

    public void addLocation(String name, TeleportTarget teleportPos){
        name = name.toLowerCase(Locale.ROOT);
        destinations.put(name,teleportPos);
        update();
    }

    public void removeLocation(String name){
        if(destinations.remove(name) == null){
            LOGGER.error("Unable to remove location with name: {}", name);
            return;
        }
        update();
    }

    public String listNames(){
        return String.join("\n", destinations.keySet());
    }

    protected abstract void update();
}
