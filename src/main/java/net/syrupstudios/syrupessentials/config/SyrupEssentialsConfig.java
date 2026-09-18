package net.syrupstudios.syrupessentials.config;

import net.syrupstudios.syruplibrary.config.ConfigSection;
import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syruplibrary.config.value.ConfigType;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.nio.file.Path;

public final class SyrupEssentialsConfig {
    public static final ConfigSpec SPEC = ConfigSpec.builder("syrup_essentials")
            .header(
                    "Syrup Essentials configuration",
                    "Run /syrupessentials reload to apply settings that do not require a restart."
            )
            .build();

    private static final ConfigValue<Boolean> REGISTER_ALIAS_AS_WELL_AS_NAMESPACE = SPEC.value(
            "register_alias_as_well_as_namespace",
            ConfigType.BOOLEAN,
            false,
            "Keeps short root commands such as /home available while command namespacing is active.\n"
                    + "This setting is ignored when register_to_namespace is false.",
            RestartRequirement.REQUIRED,
            java.util.List.of()
    );
    private static final ConfigValue<Boolean> REGISTER_TO_NAMESPACE = SPEC.value(
            "register_to_namespace",
            ConfigType.BOOLEAN,
            false,
            "Places commands below /syrupessentials to avoid collisions with other mods.",
            RestartRequirement.REQUIRED,
            java.util.List.of()
    );

    private static final ConfigSection TELEPORTATION = SPEC.section(
            "teleportation", "Travel commands and saved-destination settings.");
    private static final ConfigSection BACK = TELEPORTATION.section(
            "back", "Controls the location history used by /back.");
    private static final ConfigValue<Boolean> BACK_ENABLED = BACK.bool(
            "enabled", true, "Whether /back is available.");
    private static final ConfigValue<Integer> BACK_MAX = BACK.integer(
            "max", 10, 0, 1000,
            "Number of recent positions retained per player. Set to 0 to disable history storage.");

    private static final ConfigSection HOME = TELEPORTATION.section(
            "home", "Controls destinations owned by individual players.");
    private static final ConfigValue<Boolean> HOME_ENABLED = HOME.bool(
            "enabled", true, "Whether home commands are available.");
    private static final ConfigValue<Integer> HOME_MAX = HOME.integer(
            "max", -1, -1, 10000,
            "Maximum homes per player. Use -1 for no limit.");

    private static final ConfigSection JUMP = TELEPORTATION.section(
            "jump", "Controls the operator-only /jump command.");
    private static final ConfigValue<Boolean> JUMP_ENABLED = JUMP.bool(
            "enabled", true, "Whether /jump is available.");
    private static final ConfigValue<Integer> JUMP_MAX_DISTANCE = JUMP.integer(
            "max_distance", 128, 1, 1024,
            "Furthest block distance that /jump may target.");

    private static final ConfigSection SPAWN = TELEPORTATION.section(
            "spawn", "Controls travel to the primary world's spawn point.");
    private static final ConfigValue<Boolean> SPAWN_ENABLED = SPAWN.bool(
            "enabled", true, "Whether /spawn is available.");

    private static final ConfigSection TPA = TELEPORTATION.section(
            "tpa", "Controls consent-based teleport requests between online players.");
    private static final ConfigValue<Boolean> TPA_ENABLED = TPA.bool(
            "enabled", true, "Whether TPA request commands are available.");
    private static final ConfigValue<Integer> TPA_REQUEST_TIMEOUT = TPA.integer(
            "request_timeout", 30, 1, 3600,
            "Seconds before an unanswered teleport request expires.");

    private static final ConfigSection TPL = TELEPORTATION.section(
            "tpl", "Controls the operator command for visiting a player's latest recorded position.");
    private static final ConfigValue<Boolean> TPL_ENABLED = TPL.bool(
            "enabled", true, "Whether /teleport_last is available.");

    private static final ConfigSection TPX = TELEPORTATION.section(
            "tpx", "Controls direct operator travel between dimensions.");
    private static final ConfigValue<Boolean> TPX_ENABLED = TPX.bool(
            "enabled", true, "Whether /tpx is available.");

    private static final ConfigSection WARP = TELEPORTATION.section(
            "warp", "Controls shared server destinations managed by warp commands.");
    private static final ConfigValue<Boolean> WARP_ENABLED = WARP.bool(
            "enabled", true, "Whether warp commands are available.");

    private static final ConfigSection PERSISTENCE = SPEC.section(
            "persistence", "Background storage settings for player and world data.");
    private static final ConfigValue<Integer> AUTOSAVE_INTERVAL = PERSISTENCE.integer(
            "autosave_interval", 180, 10, 86400,
            "Seconds between periodic writes of changed data.");

    private static final Values VALUES = new Values();
    private static volatile RegisteredConfig registered;

    private SyrupEssentialsConfig() {
    }

    public static synchronized ConfigLoadResult initialize() {
        return initialize(SyrupConfigManager.getInstance());
    }

    static synchronized ConfigLoadResult initialize(SyrupConfigManager manager) {
        if (registered == null) {
            registered = manager.register(SPEC);
            return registered.initialResult();
        }
        return registered.initialResult();
    }

    public static ConfigLoadResult reload() {
        RegisteredConfig handle = registered;
        if (handle == null) {
            return initialize();
        }
        return handle.reload();
    }

    public static RegisteredConfig handle() {
        RegisteredConfig handle = registered;
        if (handle == null) {
            throw new IllegalStateException("Syrup Essentials config has not been initialized");
        }
        return handle;
    }

    public static Values get() {
        return VALUES;
    }

    public static Path getPath() {
        return handle().path();
    }

    public static final class Values {
        private static final Teleportation TELEPORTATION_VALUES = new Teleportation();
        private static final Persistence PERSISTENCE_VALUES = new Persistence();

        public boolean registerAliasAsWellAsNamespace() {
            return REGISTER_ALIAS_AS_WELL_AS_NAMESPACE.get();
        }

        public boolean registerToNamespace() {
            return REGISTER_TO_NAMESPACE.get();
        }

        public Teleportation teleportation() {
            return TELEPORTATION_VALUES;
        }

        public Persistence persistence() {
            return PERSISTENCE_VALUES;
        }
    }

    public static final class Teleportation {
        private static final Tpa TPA_VALUES = new Tpa();
        private static final Home HOME_VALUES = new Home();
        private static final Toggle WARP_VALUES = new Toggle(WARP_ENABLED);
        private static final Back BACK_VALUES = new Back();
        private static final Toggle SPAWN_VALUES = new Toggle(SPAWN_ENABLED);
        private static final Toggle TPL_VALUES = new Toggle(TPL_ENABLED);
        private static final Toggle TPX_VALUES = new Toggle(TPX_ENABLED);
        private static final Jump JUMP_VALUES = new Jump();

        public Tpa tpa() { return TPA_VALUES; }
        public Home home() { return HOME_VALUES; }
        public Toggle warp() { return WARP_VALUES; }
        public Back back() { return BACK_VALUES; }
        public Toggle spawn() { return SPAWN_VALUES; }
        public Toggle teleportLast() { return TPL_VALUES; }
        public Toggle tpx() { return TPX_VALUES; }
        public Jump jump() { return JUMP_VALUES; }
    }

    public static class Toggle {
        private final ConfigValue<Boolean> value;

        private Toggle(ConfigValue<Boolean> value) {
            this.value = value;
        }

        public boolean enabled() { return value.get(); }
    }

    public static final class Tpa extends Toggle {
        private Tpa() { super(TPA_ENABLED); }
        public int requestTimeoutSeconds() { return TPA_REQUEST_TIMEOUT.get(); }
    }

    public static final class Home extends Toggle {
        private Home() { super(HOME_ENABLED); }
        public int maxHomes() { return HOME_MAX.get(); }
    }

    public static final class Back extends Toggle {
        private Back() { super(BACK_ENABLED); }
        public int maxHistory() { return BACK_MAX.get(); }
    }

    public static final class Jump extends Toggle {
        private Jump() { super(JUMP_ENABLED); }
        public int maxDistance() { return JUMP_MAX_DISTANCE.get(); }
    }

    public static final class Persistence {
        public int autosaveIntervalSeconds() { return AUTOSAVE_INTERVAL.get(); }
        public long autosaveIntervalTicks() { return AUTOSAVE_INTERVAL.get() * 20L; }
    }
}
