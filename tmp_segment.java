    public void onPlayerShoot(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TNT) {
            return;
        }
        
        // If player is in Shahed mode, do nothing on right-click with TNT
        if (isShahed(player)) {
            return;
        }

        event.setCancelled(true);
        // If in Shahed mode and gliding — standard throw (consumes 1 TNT)
        if (isShahed(player) && player.isGliding()) {
            if (!consumeTnt(player)) {
                player.sendMessage(ChatColor.RED + "Нет динамита в инвентаре!");
                return;
            }
            Location spawnLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6));
            Vector velocity = player.getLocation().getDirection().normalize().multiply(1.5);
            player.getWorld().spawn(spawnLocation, TNTPrimed.class, tnt -> {
                tnt.setSource(player);
                tnt.setFuseTicks(60);
                tnt.setVelocity(velocity);
            });
            player.sendMessage(ChatColor.YELLOW + "Бомба запущена!");
            getLogger().info("[" + getName() + "] " + player.getName() + " threw a TNT (shahed)");
            return;
        }

        // Not Shahed — impact TNT: consume and explode on contact
        if (!consumeTnt(player)) {
            player.sendMessage(ChatColor.RED + "Нет динамита в инвентаре!");
            return;
        }
        launchImpactTnt(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isShahed(player) || !player.isGliding()) {
            return;
        }
