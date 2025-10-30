package com.pon4ikisdonut.elytrashahed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
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

    private static final float BASE_EXPLOSION_POWER = 5.0f;

    private final Map<UUID, ShahedState> shahedPlayers = new HashMap<>();
    private NamespacedKey aaKey;
    private NamespacedKey ghostFireworkKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("shahed") != null) {
            getCommand("shahed").setExecutor(new ShahedToggleCommand(this));
        }
        if (getCommand("aagun") != null) {
            getCommand("aagun").setExecutor(new AAGunCommand(this));
        }
        if (getCommand("ghostfirework") != null) {
            getCommand("ghostfirework").setExecutor(new GhostFireworkCommand(this));
        }

        aaKey = new NamespacedKey(this, "aa-gun");
        ghostFireworkKey = new NamespacedKey(this, "ghost-firework");
        int max = getMaxShahedPower();
        prettyLog("ElytraShahed", "enabled v" + getDescription().getVersion() + " (max power=" + max + ")");
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
        prettyLog("ElytraShahed", "disabled v" + getDescription().getVersion());
    }

    boolean activateShahed(Player player) {
        if (shahedPlayers.containsKey(player.getUniqueId())) {
            return false;
        }

        ItemStack previousHelmet = cloneItem(player.getInventory().getHelmet());
        player.getInventory().setHelmet(new ItemStack(Material.TNT));
        ShahedState newState = new ShahedState(previousHelmet);
        shahedPlayers.put(player.getUniqueId(), newState);
        player.sendMessage(ChatColor.RED + "Режим «Шахед» активирован! Ты заряжен, будь осторожен.");
        getLogger().info("[" + getName() + "] " + player.getName() + " activated Shahed mode");
        return true;
    }

    boolean deactivateShahed(Player player) {
        ShahedState state = shahedPlayers.remove(player.getUniqueId());
        if (state == null) {
            return false;
        }
        restoreHelmet(player, state.previousHelmet());
        player.sendMessage(ChatColor.GREEN + "Режим «Шахед» отключён.");
        getLogger().info("[" + getName() + "] " + player.getName() + " deactivated Shahed mode");
        return true;
    }

    boolean isShahed(Player player) {
        return shahedPlayers.containsKey(player.getUniqueId());
    }

    private ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    private void restoreHelmet(Player player, ItemStack itemStack) {
        player.getInventory().setHelmet(cloneItem(itemStack));
    }

    void triggerExplosion(Player player) {
        ShahedState state = shahedPlayers.remove(player.getUniqueId());
        int scale = 1;
        if (state != null) {
            restoreHelmet(player, state.previousHelmet());
            scale = Math.max(1, Math.min(getMaxShahedPower(), state.getTntScale()));
        }
        float power = BASE_EXPLOSION_POWER * scale;
        Location location = player.getLocation();
        player.getWorld().createExplosion(location, power, true, true, player);
        getLogger().info("[" + getName() + "] " + player.getName() + " exploded with power x" + scale + " (" + power + ")");
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

        getServer().getScheduler().runTask(this, () -> {
            player.setCooldown(Material.CROSSBOW, 0);
            chargeCrossbowWithAARocket(bow);
        });
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

    @EventHandler(ignoreCancelled = true)
    public void onUseGhostFirework(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();
        if (stack == null || stack.getType() != Material.FIREWORK_ROCKET) {
            return;
        }
        if (!isGhostFirework(stack)) {
            return;
        }

        event.setCancelled(true);
        if (!player.isGliding()) {
            player.sendMessage(ChatColor.YELLOW + "Этот фейерверк работает только в полёте.");
            return;
        }
        int power = 3;
        if (stack.getItemMeta() instanceof FireworkMeta meta) {
            power = Math.max(1, meta.getPower());
        }
        startGhostBoost(player, power);
        if (player.getGameMode() != GameMode.CREATIVE) {
            decrementItemInHand(player, event.getHand(), stack);
        }
        getLogger().info("[" + getName() + "] " + player.getName() + " used ghost firework boost (power=" + power + ")");
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
        if (!(event.getDamager() instanceof Firework firework)) {
            return;
        }
        if (isGhostFirework(firework)) {
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
    }

    void setShahedScale(Player player, int scale) {
        ShahedState state = shahedPlayers.get(player.getUniqueId());
        if (state != null) {
            state.setTntScale(Math.min(getMaxShahedPower(), scale));
        }
    }

    int getShahedScale(Player player) {
        ShahedState state = shahedPlayers.get(player.getUniqueId());
        int max = getMaxShahedPower();
        return state == null ? 1 : Math.max(1, Math.min(max, state.getTntScale()));
    }

    void prettyLog(String title, String message) {
        String line = "==============================";
        getLogger().info("\n" + line +
                "\n  " + title +
                "\n  " + message +
                "\n" + line);
    }

    private int getMaxShahedPower() {
        return Math.max(1, getConfig().getInt("max-shahed-power", 16));
    }

    private boolean isGhostFirework(ItemStack itemStack) {
        if (ghostFireworkKey == null || itemStack == null || itemStack.getType() != Material.FIREWORK_ROCKET) {
            return false;
        }
        if (!(itemStack.getItemMeta() instanceof FireworkMeta meta)) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(ghostFireworkKey, PersistentDataType.BYTE);
    }

    private boolean isGhostFirework(Firework firework) {
        if (ghostFireworkKey == null) {
            return false;
        }
        FireworkMeta meta = firework.getFireworkMeta();
        return meta != null && meta.getPersistentDataContainer().has(ghostFireworkKey, PersistentDataType.BYTE);
    }

    private void decrementItemInHand(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            if (slot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        if (slot == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(stack);
        } else {
            player.getInventory().setItemInMainHand(stack);
        }
    }

    private void startGhostBoost(Player player, int power) {
        double strength = 0.35D + 0.1D * Math.max(1, power);
        int duration = 12 + Math.max(1, power) * 4;
        UUID uuid = player.getUniqueId();
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
                boost.setY(boost.getY() + 0.06D * Math.max(1, power));
                Vector newVelocity = target.getVelocity().add(boost);
                double maxSpeed = 2.5D + 0.5D * Math.max(1, power);
                if (newVelocity.lengthSquared() > maxSpeed * maxSpeed) {
                    newVelocity = newVelocity.normalize().multiply(maxSpeed);
                }
                target.setVelocity(newVelocity);
                if (++ticks >= duration) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    ItemStack createGhostFirework(int amount) {
        int total = Math.max(1, Math.min(64, amount));
        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET, total);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        if (meta != null) {
            meta.setPower(3);
            meta.clearEffects();
            meta.displayName(Component.text("Бесследный фейерверк").color(NamedTextColor.GRAY));
            java.util.List<Component> lore = java.util.List.of(
                    Component.text("Без частиц и хитбокса.").color(NamedTextColor.DARK_GRAY),
                    Component.text("Использовать только в полёте.").color(NamedTextColor.DARK_GRAY)
            );
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(ghostFireworkKey, PersistentDataType.BYTE, (byte) 1);
            rocket.setItemMeta(meta);
        }
        return rocket;
    }

    void giveGhostFireworks(Player player, int amount) {
        ItemStack stack = createGhostFirework(amount);
        int issued = stack.getAmount();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(ChatColor.GRAY + "Выданы бесследные фейерверки x" + issued + ".");
        getLogger().info("[" + getName() + "] " + player.getName() + " received ghost fireworks x" + issued);
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
        player.sendMessage(ChatColor.GOLD + "Получен AA-Gun.");
        getLogger().info("[" + getName() + "] " + player.getName() + " received AA-Gun");
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
}
