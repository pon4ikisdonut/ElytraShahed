package com.pon4ikisdonut.elytrashahed;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class ShahedPlugin extends JavaPlugin implements Listener {

    private static final float DEFAULT_BASE_EXPLOSION_POWER = 5.0f;
    private static final int HELMET_SLOT = 39;
    private static final Material DEFAULT_REACTIVE_ITEM = Material.NETHERITE_SCRAP;
    private static final String PERMISSION_REACTIVE = "elytrashahed.reactive";

    private final Map<UUID, ShahedState> shahedPlayers = new HashMap<>();
    private final Set<UUID> pendingShahedDeaths = new HashSet<>();

    private NamespacedKey aaKey;
    private final LocalizationManager localization = new LocalizationManager(this);
    private Material reactiveBoostItem = DEFAULT_REACTIVE_ITEM;
    private int reactiveBoostPower = 3;
    private String craterMode = "vanilla"; // vanilla | funnel
    private double funnelRadiusPerScale = 1.2;
    private double funnelDepthPerScale = 0.6;
    private float baseExplosionPower = DEFAULT_BASE_EXPLOSION_POWER;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginSettings();

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("shahed") != null) {
            getCommand("shahed").setExecutor(new ShahedToggleCommand(this));
        }
        if (getCommand("aagun") != null) {
            getCommand("aagun").setExecutor(new AAGunCommand(this));
        }
        if (getCommand("elytrareload") != null) {
            getCommand("elytrareload").setExecutor(new ReloadConfigCommand(this));
        }

        aaKey = new NamespacedKey(this, "aa-gun");
        logStartupBanner();
    }

    @Override
    public void onDisable() {
        shahedPlayers.forEach((uuid, state) -> {
            Player player = getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreHelmet(player, state.previousHelmet());
            }
        });
        shahedPlayers.clear();
        pendingShahedDeaths.clear();
        prettyLog("ElytraShahed", "disabled v" + getDescription().getVersion());
    }

    void reloadPluginSettings() {
        reloadConfig();
        ensureLanguageFiles();

        String lang = getConfig().getString("language", "en");
        localization.load(lang);

        reactiveBoostPower = Math.max(1, getConfig().getInt("reactive-boost-power", 3));
        String itemName = getConfig().getString("reactive-boost-item", DEFAULT_REACTIVE_ITEM.name());
        reactiveBoostItem = parseMaterial(itemName, DEFAULT_REACTIVE_ITEM);

        baseExplosionPower = (float) Math.max(0.1, getConfig().getDouble("base-explosion-power", DEFAULT_BASE_EXPLOSION_POWER));
        craterMode = getConfig().getString("crater-mode", "vanilla").toLowerCase(Locale.ROOT);
        funnelRadiusPerScale = Math.max(0.1, getConfig().getDouble("funnel-radius-per-scale", 1.2));
        funnelDepthPerScale = Math.max(0.1, getConfig().getDouble("funnel-depth-per-scale", 0.6));

        if (isEnabled()) {
            getLogger().info("Settings reloaded: language=" + localization.getLanguageCode()
                    + ", reactiveItem=" + reactiveBoostItem + ", boostPower=" + reactiveBoostPower);
        }
    }

    String message(MessageKey key, Object... args) {
        return localization.format(key, args);
    }

    String getActiveLanguage() {
        return localization.getLanguageCode();
    }

    NamespacedKey getAaKey() {
        return aaKey;
    }

    boolean isShahed(Player player) {
        return shahedPlayers.containsKey(player.getUniqueId());
    }

    boolean activateShahed(Player player, int requestedScale) {
        if (isShahed(player)) {
            return false;
        }
        int scale = Math.max(1, Math.min(getMaxShahedPower(), requestedScale));
        if (!consumeTnt(player, scale)) {
            player.sendMessage(message(MessageKey.NEED_TNT, scale, scale));
            return false;
        }

        ItemStack previousHelmet = cloneItem(player.getInventory().getHelmet());
        player.getInventory().setHelmet(new ItemStack(Material.TNT));
        ShahedState newState = new ShahedState(previousHelmet, scale);
        shahedPlayers.put(player.getUniqueId(), newState);
        player.sendMessage(message(MessageKey.SHAHED_ACTIVATED));
        getLogger().info("[" + getName() + "] " + player.getName() + " activated Shahed mode x" + scale);
        return true;
    }

    boolean deactivateShahed(Player player) {
        ShahedState state = shahedPlayers.remove(player.getUniqueId());
        if (state == null) {
            return false;
        }
        restoreHelmet(player, state.previousHelmet());
        player.sendMessage(message(MessageKey.SHAHED_DEACTIVATED));
        getLogger().info("[" + getName() + "] " + player.getName() + " deactivated Shahed mode");
        return true;
    }

    boolean updateShahedScale(Player player, int requestedScale) {
        ShahedState state = shahedPlayers.get(player.getUniqueId());
        if (state == null) {
            return activateShahed(player, requestedScale);
        }
        int scale = Math.max(1, Math.min(getMaxShahedPower(), requestedScale));
        if (state.getTntScale() == scale) {
            return true;
        }
        if (!consumeTnt(player, scale)) {
            player.sendMessage(message(MessageKey.NEED_TNT, scale, scale));
            return false;
        }
        state.setTntScale(scale);
        getLogger().info("[" + getName() + "] " + player.getName() + " set Shahed scale to x" + scale);
        return true;
    }

    int getShahedScale(Player player) {
        ShahedState state = shahedPlayers.get(player.getUniqueId());
        int max = getMaxShahedPower();
        return state == null ? 1 : Math.max(1, Math.min(max, state.getTntScale()));
    }

    void giveAAGun(Player player) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW, 1);
        CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("AA-Gun").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(aaKey, PersistentDataType.BYTE, (byte) 1);
            crossbow.setItemMeta(meta);
        }
        chargeCrossbowWithAARocket(crossbow);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(crossbow);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(message(MessageKey.AAGUN_RECEIVED));
        getLogger().info("[" + getName() + "] " + player.getName() + " received AA-Gun");
    }

    @EventHandler(ignoreCancelled = true)
    public void onReactiveBoost(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() != reactiveBoostItem) {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission(PERMISSION_REACTIVE)) {
            player.sendMessage(message(MessageKey.NO_PERMISSION));
            return;
        }
        if (!player.isGliding()) {
            player.sendMessage(message(MessageKey.REACTIVE_ONLY_GLIDING));
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            int amount = handItem.getAmount();
            if (amount <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                handItem.setAmount(amount - 1);
                player.getInventory().setItemInMainHand(handItem);
            }
        }

        player.sendMessage(message(MessageKey.REACTIVE_TRIGGERED));
        startReactiveBoost(player, reactiveBoostPower);
        getLogger().info("[" + getName() + "] " + player.getName() + " used reactive boost (item=" + reactiveBoostItem + ", power=" + reactiveBoostPower + ")");
    }

    @EventHandler(ignoreCancelled = true)
    public void onAAGunShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.CROSSBOW) {
            return;
        }
        if (!(bow.getItemMeta() instanceof CrossbowMeta meta)) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(aaKey, PersistentDataType.BYTE)) {
            return;
        }

        // Folia-safe: schedule on the player's entity thread next tick
        try {
            player.getScheduler().runDelayed(this, task -> {
                player.setCooldown(Material.CROSSBOW, 0);
                chargeCrossbowWithAARocket(bow);
            }, null, 1L);
        } catch (Throwable t) {
            // Fallback for non-Folia servers
            getServer().getScheduler().runTask(this, () -> {
                player.setCooldown(Material.CROSSBOW, 0);
                chargeCrossbowWithAARocket(bow);
            });
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isShahed(player) || !player.isGliding()) {
            return;
        }

        BoundingBox playerBox = player.getBoundingBox();
        if (isTouchingSolidBlock(player, playerBox)) {
            triggerExplosion(player);
            return;
        }
        for (Entity nearby : player.getNearbyEntities(0.8, 0.8, 0.8)) {
            if (nearby.equals(player)) {
                continue;
            }
            if (nearby instanceof Firework) {
                continue;
            }
            if (playerBox.overlaps(nearby.getBoundingBox())) {
                triggerExplosion(player);
                return;
            }
        }
    }

    @EventHandler
    public void onFlyIntoWall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            return;
        }
        if (!isShahed(player)) {
            return;
        }
        triggerExplosion(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!player.isGliding() || !isShahed(player)) {
            return;
        }
        triggerExplosion(player);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShahedDamagedByFirework(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isShahed(player)) {
            return;
        }
        if (!(event.getDamager() instanceof Firework)) {
            return;
        }
        triggerExplosion(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ShahedState state = shahedPlayers.remove(player.getUniqueId());
        if (state != null) {
            restoreHelmet(player, state.previousHelmet());
        }
        pendingShahedDeaths.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (pendingShahedDeaths.remove(player.getUniqueId())) {
            event.deathMessage(Component.text(String.format(Locale.ROOT,
                    localization.format(MessageKey.SHAHED_DEATH_MESSAGE, player.getName()))));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isShahed(player)) {
            return;
        }
        if (!hasShahedHelmet(player)) {
            return;
        }
        if (event.getSlot() == HELMET_SLOT || event.getRawSlot() == HELMET_SLOT) {
            event.setCancelled(true);
            player.updateInventory();
        } else if (event.isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() == Material.TNT) {
                event.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isShahed(player) || !hasShahedHelmet(player)) {
            return;
        }
        if (event.getRawSlots().contains(HELMET_SLOT) || event.getInventorySlots().contains(HELMET_SLOT)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHelmetDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isShahed(player)) {
            return;
        }
        if (event.getItemDrop().getItemStack().getType() == Material.TNT && hasShahedHelmet(player)) {
            event.setCancelled(true);
        }
    }

    void triggerExplosion(Player player) {
        ShahedState state = shahedPlayers.remove(player.getUniqueId());
        int scale = 1;
        if (state != null) {
            restoreHelmet(player, state.previousHelmet());
            scale = Math.max(1, Math.min(getMaxShahedPower(), state.getTntScale()));
        }
        float power = baseExplosionPower * scale;
        Location location = player.getLocation();
        player.getWorld().createExplosion(location, power, true, true, player);
        if ("funnel".equalsIgnoreCase(craterMode)) {
            tryCarveFunnelCrater(location, scale);
        }
        pendingShahedDeaths.add(player.getUniqueId());
        player.damage(1000.0, player);
        getLogger().info("[" + getName() + "] " + player.getName() + " exploded with power x" + scale + " (" + power + ")");
    }

    private boolean consumeTnt(Player player, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack probe = new ItemStack(Material.TNT, amount);
        if (!inv.containsAtLeast(probe, amount)) {
            return false;
        }
        int remaining = amount;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() != Material.TNT) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                inv.setItem(i, null);
            }
            remaining -= take;
        }
        return true;
    }

    private boolean hasShahedHelmet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.getType() == Material.TNT;
    }

    private void startReactiveBoost(Player player, int configuredPower) {
        final int power = Math.max(1, configuredPower);
        final double strength = 0.35D + 0.1D * power;
        final double verticalBoost = 0.06D * power;
        final double maxSpeed = 2.5D + 0.5D * power;
        final int duration = 12 + power * 4;
        final UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                Player target = getServer().getPlayer(uuid);
                if (target == null || !target.isOnline()) {
                    cancel();
                    return;
                }
                if (!target.isGliding()) {
                    cancel();
                    return;
                }
                Vector direction = target.getLocation().getDirection();
                if (direction.lengthSquared() < 1.0E-4) {
                    direction = new Vector(0, 0, 0);
                } else {
                    direction = direction.normalize();
                }
                Vector boost = direction.multiply(strength);
                boost.setY(boost.getY() + verticalBoost);
                Vector newVelocity = target.getVelocity().add(boost);
                if (newVelocity.lengthSquared() > maxSpeed * maxSpeed) {
                    newVelocity = newVelocity.normalize().multiply(maxSpeed);
                }
                target.setVelocity(newVelocity);
                if (++ticks >= duration) {
                    cancel();
                }
            }
        };

        // Folia-safe scheduling for per-tick boost
        try {
            player.getScheduler().runAtFixedRate(this, task -> runnable.run(), null, 0L, 1L);
        } catch (Throwable t) {
            // Fallback to Bukkit scheduler on non-Folia
            new BukkitRunnable() {
                @Override
                public void run() { runnable.run(); }
            }.runTaskTimer(this, 0L, 1L);
        }
    }

    private ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    private void restoreHelmet(Player player, ItemStack itemStack) {
        player.getInventory().setHelmet(cloneItem(itemStack));
    }

    private void chargeCrossbowWithAARocket(ItemStack crossbow) {
        if (!(crossbow.getItemMeta() instanceof CrossbowMeta meta)) {
            return;
        }
        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET, 1);
        FireworkMeta fireworkMeta = (FireworkMeta) rocket.getItemMeta();
        if (fireworkMeta != null) {
            fireworkMeta.setPower(3);
            fireworkMeta.addEffect(org.bukkit.FireworkEffect.builder()
                    .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.RED)
                    .withFade(Color.RED)
                    .flicker(true)
                    .trail(true)
                    .build());
            rocket.setItemMeta(fireworkMeta);
        }

        List<ItemStack> projectiles = new ArrayList<>();
        projectiles.add(rocket);
        meta.setChargedProjectiles(projectiles);
        crossbow.setItemMeta(meta);
    }

    private boolean isTouchingSolidBlock(Player player, BoundingBox playerBox) {
        BoundingBox expanded = playerBox.clone().expand(0.05);
        World world = player.getWorld();
        int minX = (int) Math.floor(expanded.getMinX());
        int maxX = (int) Math.floor(expanded.getMaxX());
        int minY = (int) Math.floor(expanded.getMinY());
        int maxY = (int) Math.floor(expanded.getMaxY());
        int minZ = (int) Math.floor(expanded.getMinZ());
        int maxZ = (int) Math.floor(expanded.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!shouldTriggerOnBlock(block)) {
                        continue;
                    }
                    VoxelShape shape = block.getCollisionShape();
                    if (shape != null && !shape.getBoundingBoxes().isEmpty()) {
                        for (BoundingBox local : shape.getBoundingBoxes()) {
                            BoundingBox worldBox = new BoundingBox(
                                    local.getMinX() + x,
                                    local.getMinY() + y,
                                    local.getMinZ() + z,
                                    local.getMaxX() + x,
                                    local.getMaxY() + y,
                                    local.getMaxZ() + z
                            );
                            if (expanded.overlaps(worldBox)) {
                                return true;
                            }
                        }
                    } else {
                        BoundingBox worldBox = new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
                        if (expanded.overlaps(worldBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean shouldTriggerOnBlock(Block block) {
        Material type = block.getType();
        return !type.isAir() && !block.isPassable();
    }

    private int getMaxShahedPower() {
        return Math.max(1, getConfig().getInt("max-shahed-power", 16));
    }

    private void tryCarveFunnelCrater(Location center, int scale) {
        World world = center.getWorld();
        if (world == null) return;
        int originX = center.getBlockX();
        int originY = center.getBlockY();
        int originZ = center.getBlockZ();

        int depth = (int) Math.max(1, Math.round(scale * funnelDepthPerScale));
        double baseRadius = Math.max(1.0, scale * funnelRadiusPerScale);

        for (int dy = 0; dy <= depth; dy++) {
            int y = originY - dy;
            if (y < world.getMinHeight()) break;
            double layerFactor = 1.0 - (dy / (double) Math.max(1, depth));
            double radius = Math.max(1.0, baseRadius * (0.3 + 0.7 * layerFactor));
            int r = (int) Math.ceil(radius);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if ((dx * dx + dz * dz) <= radius * radius) {
                        Block b = world.getBlockAt(originX + dx, y, originZ + dz);
                        Material type = b.getType();
                        if (!type.isAir() && type != Material.BEDROCK && type != Material.END_PORTAL_FRAME) {
                            b.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String normalized = name.trim();
        try {
            return Material.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Material matched = Material.matchMaterial(normalized);
            if (matched != null) {
                return matched;
            }
            getLogger().warning("Unknown material '" + name + "' in config. Using " + fallback);
            return fallback;
        }
    }

    private void ensureLanguageFiles() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Unable to create plugin data folder for language files.");
        }
        File langDir = new File(dataFolder, "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            getLogger().warning("Unable to create lang directory; localization may fail.");
            return;
        }
        String[] defaults = {"en", "ru", "uk", "pl", "de", "kk", "es"};
        for (String code : defaults) {
            File target = new File(langDir, code + ".yml");
            if (!target.exists()) {
                saveResource("lang/" + code + ".yml", false);
            }
        }
    }

    private void logStartupBanner() {
        prettyLog("ElytraShahed", "enabled v" + getDescription().getVersion()
                + " (max power=" + getMaxShahedPower()
                + ", lang=" + localization.getLanguageCode()
                + ", reactiveItem=" + reactiveBoostItem + ")");
    }

    void prettyLog(String title, String message) {
        String line = "==============================";
        getLogger().info("\n" + line +
                "\n  " + title +
                "\n  " + message +
                "\n" + line);
    }
}

