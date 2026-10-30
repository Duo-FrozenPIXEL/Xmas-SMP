package de.xmassmp;

import de.xmassmp.effect.EffectSink;
import de.xmassmp.effect.ParticleSpec;
import de.xmassmp.event.EventHost;
import de.xmassmp.item.ItemCatalog;
import de.xmassmp.item.ItemSpec;
import de.xmassmp.platform.Platform;
import de.xmassmp.platform.PlayerHandle;
import de.xmassmp.platform.TotemHost;
import de.xmassmp.platform.WorldRef;
import de.xmassmp.platform.XmasLogger;
import de.xmassmp.util.BlockPos;
import de.xmassmp.util.Vec3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Plattform für Kern-Tests: Ordner im Temp-Verzeichnis, Fake-Spieler, protokollierte Ausgaben. */
public final class TestPlatform implements Platform {

    public static final WorldRef WORLD = new WorldRef("overworld");

    private final Path dir;
    private final Map<UUID, FakePlayer> online = new LinkedHashMap<>();
    /** Partikel ({@code p:<typ>}) und Sounds ({@code s:<id>}) in Aufrufreihenfolge. */
    public final List<String> effects = new ArrayList<>();
    public final List<String> errors = new ArrayList<>();
    /** Gesetzte Totem-Blöcke ({@code welt@x y z}). */
    public final Set<String> totems = new HashSet<>();
    /** Blockstellen, an denen {@link TotemHost#place} scheitert. */
    public final Set<String> blocked = new HashSet<>();
    public int craftingRefreshes;

    public TestPlatform(Path dir) {
        this.dir = dir;
    }

    public FakePlayer join(String name) {
        FakePlayer player = new FakePlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
        online.put(player.uuid(), player);
        return player;
    }

    public void quit(FakePlayer player) {
        online.remove(player.uuid());
    }

    @Override
    public Path configDirectory() {
        return dir;
    }

    @Override
    public Path dataDirectory() {
        return dir.resolve("data");
    }

    @Override
    public XmasLogger logger() {
        return new XmasLogger() {
            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
            }

            @Override
            public void error(String message, Throwable error) {
                errors.add(message + ": " + error);
            }
        };
    }

    @Override
    public EffectSink effectSink() {
        return new EffectSink() {
            @Override
            public void particle(WorldRef world, Vec3 position, ParticleSpec spec, int count) {
                effects.add("p:" + spec.type());
            }

            @Override
            public void sound(WorldRef world, Vec3 position, String sound, float volume, float pitch) {
                effects.add("s:" + sound);
            }
        };
    }

    @Override
    public EventHost eventHost() {
        return new EventHost() {
            @Override
            public void broadcast(String legacyText) {
            }

            @Override
            public void title(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            }

            @Override
            public void playSound(String sound, float volume, float pitch) {
            }

            @Override
            public void runCommand(String command) {
            }
        };
    }

    @Override
    public TotemHost totemHost() {
        return new TotemHost() {
            @Override
            public boolean place(WorldRef world, BlockPos pos) {
                String key = world.id() + "@" + pos;
                return !blocked.contains(key) && totems.add(key);
            }

            @Override
            public boolean isPresent(WorldRef world, BlockPos pos) {
                return totems.contains(world.id() + "@" + pos);
            }

            @Override
            public void remove(WorldRef world, BlockPos pos) {
                totems.remove(world.id() + "@" + pos);
            }
        };
    }

    @Override
    public void refreshCrafting() {
        craftingRefreshes++;
    }

    @Override
    public List<PlayerHandle> onlinePlayers() {
        return List.copyOf(online.values());
    }

    @Override
    public Optional<PlayerHandle> player(UUID uuid) {
        return Optional.ofNullable(online.get(uuid));
    }

    /** Spieler, dessen Zustand Tests direkt setzen und auslesen. */
    public static final class FakePlayer implements PlayerHandle {
        private final UUID uuid;
        private final String name;
        public Vec3 position = new Vec3(0, 64, 0);
        public Vec3 look = new Vec3(0, 0, 1);
        public boolean sneaking;
        public final List<String> actionBar = new ArrayList<>();
        public final List<String> messages = new ArrayList<>();
        /** Vereinfachtes Inventar: eine Liste von Stacks, höchstens {@link #capacity} Einträge. */
        public final List<ItemSpec> inventory = new ArrayList<>();
        public int capacity = 36;
        /** Items, die im Umkreis auf dem Boden liegen. */
        public final List<ItemSpec> dropped = new ArrayList<>();
        /** Inhalt eines gerade geöffneten Behälters. */
        public final List<ItemSpec> openContainer = new ArrayList<>();
        /** Item in der Haupthand und Anzahl; {@code null} = leer. */
        public ItemSpec mainHand;
        public int mainHandCount;
        public int selectedSlot;
        public boolean alive = true;
        public boolean creative;
        /** Titel als {@code titel|untertitel}. */
        public final List<String> titles = new ArrayList<>();
        public int tabRefreshes;

        FakePlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID uuid() {
            return uuid;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public WorldRef world() {
            return WORLD;
        }

        @Override
        public Vec3 position() {
            return position;
        }

        @Override
        public Vec3 eyePosition() {
            return position.add(0, 1.62, 0);
        }

        @Override
        public Vec3 lookDirection() {
            return look;
        }

        @Override
        public boolean isSneaking() {
            return sneaking;
        }

        @Override
        public void sendActionBar(String legacyText) {
            actionBar.add(legacyText);
        }

        @Override
        public void sendMessage(String legacyText) {
            messages.add(legacyText);
        }

        @Override
        public void giveItem(ItemSpec item) {
            (inventory.size() < capacity ? inventory : dropped).add(item);
        }

        /** Gleiche Regeln wie der Fabric-Adapter, siehe {@link PlayerHandle#syncCrystalItem}. */
        @Override
        public void syncCrystalItem(ItemSpec expected, boolean soulbound) {
            boolean kept = false;
            for (int i = 0; i < inventory.size(); i++) {
                ItemSpec stack = inventory.get(i);
                if (!ItemCatalog.isOwnedCrystal(stack.data())) {
                    continue;
                }
                boolean mine = uuid.toString().equals(stack.data().get(ItemCatalog.DATA_OWNER));
                boolean outdated = expected != null && !stack.data().equals(expected.data());
                if (mine && expected != null && !kept) {
                    kept = true;
                    if (outdated) {
                        inventory.set(i, expected);
                    }
                } else if (soulbound) {
                    inventory.remove(i--);
                } else if (mine && outdated) {
                    inventory.set(i, expected);
                }
            }
            if (soulbound) {
                openContainer.removeIf(stack -> ItemCatalog.isOwnedCrystal(stack.data()));
                if (!kept && expected != null && inventory.size() < capacity) {
                    inventory.add(expected);
                }
            }
        }

        @Override
        public void removeDroppedCrystals(double radius) {
            dropped.removeIf(stack -> ItemCatalog.isOwnedCrystal(stack.data()));
        }

        /** Legt {@code count} Items in die Haupthand. */
        public void hold(ItemSpec item, int count) {
            mainHand = item;
            mainHandCount = count;
        }

        @Override
        public String mainHandItem() {
            if (mainHand == null || mainHandCount <= 0) {
                return "minecraft:air";
            }
            return mainHand.isVanilla() ? mainHand.baseItem()
                    : ItemCatalog.itemId(mainHand.data().get(ItemSpec.DATA_TYPE));
        }

        @Override
        public Map<String, String> mainHandData() {
            return mainHand == null || mainHandCount <= 0 ? Map.of() : mainHand.data();
        }

        @Override
        public int selectedSlot() {
            return selectedSlot;
        }

        @Override
        public boolean consumeMainHand(String itemId, Map<String, String> requiredData) {
            if (!itemId.equals(mainHandItem())) {
                return false;
            }
            Map<String, String> data = mainHandData();
            boolean matches = requiredData.isEmpty() ? data.isEmpty()
                    : requiredData.entrySet().stream().allMatch(e -> e.getValue().equals(data.get(e.getKey())));
            if (!matches) {
                return false;
            }
            if (--mainHandCount <= 0) {
                mainHand = null;
            }
            return true;
        }

        @Override
        public void dropItem(ItemSpec item) {
            dropped.add(item);
        }

        @Override
        public void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            titles.add(title + "|" + subtitle);
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public boolean isCreative() {
            return creative;
        }

        @Override
        public void refreshTabListName() {
            tabRefreshes++;
        }

        public List<ItemSpec> crystals() {
            return inventory.stream().filter(stack -> ItemCatalog.TYPE_CRYSTAL.equals(stack.data().get(ItemSpec.DATA_TYPE)))
                    .toList();
        }

        public String lastActionBar() {
            return actionBar.isEmpty() ? null : actionBar.get(actionBar.size() - 1);
        }
    }
}
