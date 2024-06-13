package extinguisher.extinguisher;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;

public final class Extinguisher extends JavaPlugin implements Listener, CommandExecutor {
    private final ItemStack extinguisher = new ItemStack(Material.RED_CANDLE);
    private final ItemMeta meta = extinguisher.getItemMeta();
    private final NamespacedKey nsKey = new NamespacedKey(this, "extinguisher");
    private final int durability = 50;
    private final HashMap<Block, ItemMeta> blocks = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        assert meta != null;

        meta.setDisplayName(ChatColor.RED + "Extinguisher");
        meta.setCustomModelData(durability);
        meta.setLore(Collections.singletonList(String.format(ChatColor.GRAY + "Durability: %d/%d", durability, durability)));

        extinguisher.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(nsKey, extinguisher);

        recipe.shape(" X ", "XYX", " X ");
        recipe.setIngredient('X', Material.IRON_INGOT);
        recipe.setIngredient('Y', Material.WATER_BUCKET);

        getServer().addRecipe(recipe);

        getLogger().info("loaded Extinguisher");
    }

    @Override
    public void onDisable() {
        getServer().removeRecipe(nsKey);
        getLogger().info("unloaded Extinguisher");
    }

    @EventHandler
    public void placeBlock(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getBlockData().getMaterial() == Material.RED_CANDLE) {
            ItemMeta meta = event.getItemInHand().getItemMeta();

            if (meta != null) {
                blocks.put(block, meta);
            }
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getBlockData().getMaterial() == Material.RED_CANDLE) {
            if (blocks.containsKey(block)) {
                extinguisher.setItemMeta(blocks.get(block));
            } else {
                ItemMeta meta = extinguisher.getItemMeta();
                assert meta != null;

                meta.setDisplayName(ChatColor.RED + "Extinguisher");
                meta.setCustomModelData(durability);
                meta.setLore(Collections.singletonList(String.format(ChatColor.GRAY + "Durability: %d/%d", durability, durability)));

                extinguisher.setItemMeta(meta);
            }

            event.getPlayer().getWorld().dropItemNaturally(block.getLocation(), extinguisher);

            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getPlayer().isSneaking()) {
            return;
        }

        if (event.getMaterial() == Material.RED_CANDLE && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Player player = event.getPlayer();
            Location location = player.getLocation();
            World world = location.getWorld();

            double yawAngle = Math.PI * 2D * (location.getYaw() + 180D) / 360D;
            double pitchAngle = Math.PI * 2D * (location.getPitch() + 180D) / 360D;

            Location spawnLocation = new Location(
                    world,
                    location.getX() + 7 * Math.sin(yawAngle),
                    location.getY() + 7 * Math.sin(pitchAngle),
                    location.getZ() - 7 * Math.cos(yawAngle)
            );

            int x = (int)spawnLocation.getX();
            int y = (int)spawnLocation.getY();
            int z = (int)spawnLocation.getZ();

            int multiplierX = x > 0 ? 1 : -1;
            int multiplierY = y > 0 ? 1 : -1;
            int multiplierZ = z > 0 ? 1 : -1;

            for (int numX = -5; numX <= 5; numX++) {
                for (int numY = -5; numY <= 5; numY++) {
                    for (int numZ = -5; numZ <= 5; numZ++) {
                        assert world != null;
                        Block block = world.getBlockAt(x + numX * multiplierX, y + numY * multiplierY, z + numZ * multiplierZ);
                        Material material = block.getBlockData().getMaterial();

                        if (material == Material.FIRE || material == Material.SOUL_FIRE) {
                            block.breakNaturally();
                        } else if (material.name().contains("CANDLE")) {
                            block.setType(material);
                        }

                        for (Player target: world.getPlayers()) {
                            Location targetLocation = target.getLocation();

                            if (targetLocation.getBlockX() == x + numX * multiplierX && targetLocation.getBlockY() == y + numY * multiplierY && targetLocation.getBlockZ() == z + numZ * multiplierZ) {
                                target.setVelocity(player.getLocation().getDirection().multiply(.5));
                            }
                        }
                    }
                }
            }

            world.spawnParticle(Particle.EXPLOSION_HUGE, spawnLocation, 10, 0, 0, 0);
            world.playEffect(location, Effect.EXTINGUISH, 1);

            if (!player.isSprinting()) {
                player.setVelocity(player.getLocation().getDirection().multiply(-.5));
            }

            ItemStack item = event.getItem();
            assert item != null;
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (!meta.hasCustomModelData()) {
                    meta.setCustomModelData(durability);
                }

                meta.setDisplayName(ChatColor.RED + "Extinguisher");
                meta.setCustomModelData(meta.getCustomModelData() - 1);
                meta.setLore(Collections.singletonList(String.format(ChatColor.GRAY + "Durability: %d/%d", meta.getCustomModelData(), durability)));

                item.setItemMeta(meta);

                if (meta.getCustomModelData() < 0) {
                    player.getWorld().playEffect(location, Effect.ANVIL_BREAK, 1);
                    player.getInventory().remove(item);
                }
            }

            event.setCancelled(true);
        }
    }
}
