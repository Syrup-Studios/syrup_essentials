package net.syrupstudios.syrupessentials.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.syrupstudios.syruplibrary.teleport.TeleportTarget;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class Homes extends Locations {
    private boolean requireUpdate = false;

    @Override
    protected void update() {
        this.requireUpdate = true;
    }

    public Homes(){
        super(new HashMap<>());
    }

    public Homes(Map<String, TeleportTarget> destinations) {
        super(new HashMap<>(destinations));
    }

    public boolean requiresUpdate(){
        return this.requireUpdate;
    }

    public void clearUpdate() { this.requireUpdate = false; }

    public static final Codec<Homes> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.unboundedMap(Codec.STRING, TeleportTarget.CODEC)
                    .fieldOf("destinations")
                    .forGetter(Locations::getDestinations)
    ).apply(builder, Homes::new));

}
