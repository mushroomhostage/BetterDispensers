/*
Copyright (c) 2012, Mushroom Hostage
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package me.exphc.BetterDispensers;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.lang.Byte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.Material.*;
import org.bukkit.material.MaterialData;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.Vector;
import org.bukkit.*;

import org.bukkit.craftbukkit.entity.CraftArrow;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftServer;

class BetterDispensersProjectileHitTask implements Runnable {
    Entity entity;
    BetterDispensers plugin;

    public BetterDispensersProjectileHitTask(Entity entity, BetterDispensers plugin) {
        this.entity = entity;
        this.plugin = plugin;
    }

    public void run() {
        if (!(entity instanceof Arrow)) {
            // TODO: support non-arrow projectiles, like snowballs
            // but, seems we would have to do the raytracing ourselves - see http://forums.bukkit.org/threads/solved-on-how-to-get-the-block-an-arrow-lands-in.55768/#post-1080442
            return;
        }

        Arrow arrow = (Arrow)entity;

        Block block = getArrowHit(arrow);

        if (block.getType() != Material.DISPENSER) {
            return;
        }

        BlockState blockState = block.getState();
        if (!(blockState instanceof Dispenser)) {
            return;
        }

        Dispenser dispenser = (Dispenser)blockState;
        Inventory inventory = dispenser.getInventory();

        int functions = plugin.listener.getDispenserFunctions(block);

        net.minecraft.server.EntityArrow entityArrow = ((CraftArrow)arrow).getHandle();

        // Activates when hit with arrows
        if (entityArrow.fromPlayer && plugin.getConfig().getBoolean("dispenser.dispenseOnPlayerArrows", true) ||
            !entityArrow.fromPlayer && plugin.getConfig().getBoolean("dispenser.dispenseOnNonPlayerArrows", true)) {
            dispenser.dispense();
        }

        if ((functions & BetterDispensersListener.FUNCTION_VACUUM) != 0) {
            // TODO: option to always enable, for compatibility with 1.x

            // Add arrows to inventory, if infinite/finite as configured
            if (entityArrow.fromPlayer && plugin.getConfig().getBoolean("vacuum.enablePlayerArrows", true) ||
                !entityArrow.fromPlayer && plugin.getConfig().getBoolean("vacuum.enableNonPlayerArrows", false)) {

                HashMap<Integer,ItemStack> excess = inventory.addItem(new ItemStack(Material.ARROW, 1));
                if (excess.size() == 0) {
                    // successfully added to inventory, so remove entity
                    arrow.remove();
                }
            }

            // TODO: accept other items! on drop, or item spawn? for Buildcraft-like transport
        }
    }

    // Get the block an arrow hit [from EnchantMore]
    // see http://forums.bukkit.org/threads/on-how-to-get-the-block-an-arrow-lands-in.55768/#post-954542
    public Block getArrowHit(Arrow arrow) {
        World world = arrow.getWorld();

        net.minecraft.server.EntityArrow entityArrow = ((CraftArrow)arrow).getHandle();

        try {
            // saved to NBT tag as xTile,yTile,zTile
            Field fieldX = net.minecraft.server.EntityArrow.class.getDeclaredField("e");
            Field fieldY = net.minecraft.server.EntityArrow.class.getDeclaredField("f");
            Field fieldZ = net.minecraft.server.EntityArrow.class.getDeclaredField("g");

            fieldX.setAccessible(true);
            fieldY.setAccessible(true);
            fieldZ.setAccessible(true);

            int x = fieldX.getInt(entityArrow);
            int y = fieldY.getInt(entityArrow);
            int z = fieldZ.getInt(entityArrow);

            return world.getBlockAt(x, y, z);
        } catch (Exception e) {
            plugin.log("getArrowHit("+arrow+" reflection failed: "+e);
            throw new IllegalArgumentException(e);
        }
    }

}

// Task to set dispenser orientation
class BetterDispensersOrientTask implements Runnable {
    byte data;
    Block block;
    BetterDispensers plugin;

    public BetterDispensersOrientTask(byte data, Block block, BetterDispensers plugin) {
        this.data = data;
        this.block = block;
        this.plugin = plugin;
    }

    public void run() {
        BlockState blockState = block.getState();
        blockState.setData(new MaterialData(Material.DISPENSER.getId(), data));
        blockState.update(true);
    }
}

class BetterDispensersListener implements Listener {
    BetterDispensers plugin;
    Random random;

    // Directions to check around dispenser
    final BlockFace[] surfaceDirections = { 
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN };

    // Player for performing actions from the dispenser
    net.minecraft.server.EntityPlayer fakePlayer;

    ConcurrentHashMap<Player,Dispenser> openedCrafters;

    public BetterDispensersListener(BetterDispensers plugin) {
        this.plugin = plugin;

        this.random = new Random();

        this.openedCrafters = new ConcurrentHashMap<Player,Dispenser>();

        net.minecraft.server.MinecraftServer console = ((CraftServer)Bukkit.getServer()).getServer();
        net.minecraft.server.ItemInWorldManager manager = new net.minecraft.server.ItemInWorldManager(console.getWorldServer(0));

        fakePlayer = new net.minecraft.server.EntityPlayer(
            console,
            ((CraftWorld)Bukkit.getWorlds().get(0)).getHandle(), // TODO: does it need to be in each world?
            "[BetterDispensers]",
            manager);


        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Get bit mask of the configured 'functions' of the dispenser based on its surroundings
    public int getDispenserFunctions(Block origin) {
        int functions = 0;

        for (BlockFace direction: surfaceDirections) {
            Block near = origin.getRelative(direction);

            int id = near.getTypeId();
            if (id == plugin.getConfig().getInt("crafter.blockID", 58 /* crafting table */)) {
                functions |= FUNCTION_CRAFTER;
            } else if (id == plugin.getConfig().getInt("interactor.blockID", 22 /* lapis block */)) {
                functions |= FUNCTION_INTERACTOR;
            } else if (id == plugin.getConfig().getInt("breaker.blockID", 42 /* iron block */)) {
                functions |= FUNCTION_BREAKER;
            } else if (id == plugin.getConfig().getInt("vacuum.blockID", 49 /* obsidian */)) {
                functions |= FUNCTION_VACUUM;
            } 
            // TODO: if any kind of container block (chest = 54, modded chest, another dispenser) 
            // functions |= FUNCTION_STORAGE
        }

        return functions;
    }

    // accept arrows inside dispensers
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();

        // must schedule a task since arrow collision detection hasn't happened yet
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new BetterDispensersProjectileHitTask(entity, plugin));
    }

    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER) {
            return;
        }

        Block blockAgainst = event.getBlockAgainst();
        Player player = event.getPlayer();

        plugin.log("placed "+block.getLocation()+" by "+player.getLocation());

        // Intelligently set orientation like pistons do
        int l = plugin.determineOrientation(block.getLocation(), player);
        if (l != -1) {
            byte data = (byte)l;

            if (plugin.getConfig().getBoolean("tellPlayer", true)) {
                player.sendMessage("Placed dispenser facing "+plugin.dirToString(data));
            }


            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new BetterDispensersOrientTask(data, block, plugin));
        }
    }

    public static final int FUNCTION_CRAFTER    = 1 << 0;
    public static final int FUNCTION_INTERACTOR = 1 << 1;
    public static final int FUNCTION_BREAKER    = 1 << 2;
    public static final int FUNCTION_VACUUM     = 1 << 3;
    public static final int FUNCTION_STORAGE    = 1 << 4;

    // handle up/down dispensers
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockDispense(BlockDispenseEvent event) {
        plugin.log("dispense"+event);
        Block block = event.getBlock();
        BlockState blockState = block.getState();
        if (!(blockState instanceof Dispenser)) {
            return;
        }

        Dispenser dispenser = (Dispenser)blockState;

        // Override ALL dispensing everywhere and do it ourselves
        event.setCancelled(true);

        int functions = getDispenserFunctions(block);
        plugin.log("functions="+functions);

        net.minecraft.server.World world = ((CraftWorld)block.getWorld()).getHandle();
        int x = block.getX(), y = block.getY(), z = block.getZ();

        net.minecraft.server.TileEntityDispenser tileEntity = (net.minecraft.server.TileEntityDispenser)world.getTileEntity(x, y, z);
        if (tileEntity == null) {
            plugin.log("no dispenser tile entity at "+block);
            return;
        }

        net.minecraft.server.ItemStack item;

        if ((functions & FUNCTION_CRAFTER) != 0) {
            // Craft dispenser matrix into crafting matrix
            ItemStack[] contents = dispenser.getInventory().getContents();

            net.minecraft.server.PlayerInventory playerInventory = new net.minecraft.server.PlayerInventory(null);

            net.minecraft.server.ContainerDispenser container = new net.minecraft.server.ContainerDispenser(playerInventory, tileEntity);

            net.minecraft.server.InventoryCrafting matrix = new net.minecraft.server.InventoryCrafting(container, 3, 3);
            matrix.resultInventory = new net.minecraft.server.InventoryCraftResult();

            for (int i = 0; i < contents.length; i += 1) {
                if (contents[i] != null) {
                    matrix.setItem(i, ((CraftItemStack)contents[i]).getHandle());
                }
            }

            // Dispense crafting result
            item = net.minecraft.server.CraftingManager.getInstance().craft(matrix);

            if (item == null) {
                // not a craftable recipe
                world.triggerEffect(1001, x, y, z, 0);   // "failed to dispense" effect, empty click
                event.setCancelled(true);
                return;
            }

            plugin.log("CRAFT: " + item);

            // Take one from all slots
            for (int i = 0; i < contents.length; i += 1) {
                tileEntity.splitStack(i, 1);
            }

            dispenseItem(blockState, world, item, x, y, z);
        } else if ((functions & FUNCTION_INTERACTOR) != 0) {
            // Use item, i.e., like right-clicking hoe tills
            int slot = tileEntity.findDispenseSlot();
            // TODO: take tool damage if is a tool, instead of removing!
            item = tileEntity.splitStack(slot, 1);

            int face = 0;    // TODO: top?

            // TODO: use actual direction! and have a reach, like player!
            int ax = x, ay = y, az = z+1;
            plugin.log("DEPLOY at "+ax+","+ay+","+az);

            net.minecraft.server.Item.byId[item.id].interactWith(item, fakePlayer, world, ax, ay, az, face);

            Block b = Bukkit.getWorlds().get(0).getBlockAt(ax,ay,az); 
            plugin.log("block "+b);

            // TODO: check if entities nearby, for right-clicking on (i.e., shears on sheep)
        } else if ((functions & FUNCTION_BREAKER) != 0) {
            int slot = tileEntity.findDispenseSlot();

            tileEntity.getItem(slot).damage(1, fakePlayer);

            // TODO: use actual direction! and same reach as player, like interactor
            int ax = x, ay = y, az = z+1;

            Block b = Bukkit.getWorlds().get(0).getBlockAt(ax,ay,az);   // TODO: multi-world support!
            plugin.log("block "+b);
            // TODO: can this send block break events, so plugins (like EnchantMore) can handle the break?
            // and, it should respect world protection too.. (with fake user)
            b.breakNaturally(new CraftItemStack(tileEntity.getItem(slot)));
        } else {
            // Regular dispensing.. but possibly vertical
            int slot = tileEntity.findDispenseSlot();

            item = tileEntity.splitStack(slot, 1);
            dispenseItem(blockState, world, item, x, y, z);
        }
    }

    // Dispense an item ourselves
    // See net/minecraft/server/BlockDispenser.java dispense()
    private void dispenseItem(BlockState blockState, net.minecraft.server.World world, net.minecraft.server.ItemStack item, int x, int y, int z) {
        byte data = blockState.getRawData();
    
        double b0 = 0, b1 = 0, dy = 0;
        double v = plugin.getConfig().getDouble("dispenser.velocityHorizontal", 0.1);

        switch (data) {
        case 0:     // down
            v = plugin.getConfig().getDouble("dispenser.velocityDown", -0.05);
            dy = -1.0;
            break;
        case 1:     // up
            v = plugin.getConfig().getDouble("dispenser.velocityUp", 0.4);
            dy = 1.0;
            break;
        case 2:     // north
            b1 = -1.0;
            break;
        case 3:     // south
            b1 = 1.0;
            break;
        default:    // 6-15 unused
            plugin.log("warning: unknown data value: " + data);
            // vanilla falls through to west
        case 4:     // west
            b0 = -1.0;
            break;
        case 5:     // east
            b0 = 1.0;
            break;
        }


        plugin.log("dispensing item "+item);
        if (item == null) {
            world.triggerEffect(1001, x, y, z, 0);   // "failed to dispense" effect, empty click
            return;
        }

        net.minecraft.server.Entity entity = null;

        // Create new entity at center of block face
        double x0 = x + b0*0.6 + 0.5;       // d0
        double y0 = y + 0.5 + dy;           // d1
        double z0 = z + b1*0.6 + 0.5;       // d2

        if (item.id == net.minecraft.server.Item.ARROW.id) {
            net.minecraft.server.EntityArrow arrow = new net.minecraft.server.EntityArrow(world, x0, y0, z0);
            arrow.shoot(b0, v, b1, 1.1f, 6.0f);
            arrow.fromPlayer = true;
            entity = (net.minecraft.server.Entity)arrow;
        } else if (item.id == net.minecraft.server.Item.EGG.id) {
            net.minecraft.server.EntityEgg egg = new net.minecraft.server.EntityEgg(world, x0, y0, z0);
            egg.a(b0, v, b1, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)egg;
        } else if (item.id == net.minecraft.server.Item.SNOW_BALL.id) {
            net.minecraft.server.EntitySnowball ball = new net.minecraft.server.EntitySnowball(world, x0, y0, z0);
            ball.a(b0, v, b1, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)ball;

        /* TODO: add TNT cannons (optional)
        not as simple as others, because TNTPrimed isn't a Projectile.. so can't use a()
        } else if (item.id == net.minecraft.server.Block.TNT.id && plugin.getConfig().getBoolean("primeTNT", false)) {
            net.minecraft.server.EntityTNTPrimed tnt = new net.minecraft.server.EntityTNTPrimed(world, x0, y0, z0);
            tnt.a(0, v, 0, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)tnt;
        */
        } else if (item.id == net.minecraft.server.Item.POTION.id && net.minecraft.server.ItemPotion.c(item.getData())) {
            // splash potion
            net.minecraft.server.EntityPotion potion = new net.minecraft.server.EntityPotion(world, x0, y0, z0, item.getData());
            potion.a(b0, v, b1, 1.375f, 6.0f);     // why 1.375 not 1.1? because Minecraft
            entity = (net.minecraft.server.Entity)potion;
        } else if (item.id == net.minecraft.server.Item.EXP_BOTTLE.id) {
            net.minecraft.server.EntityThrownExpBottle bottle = new net.minecraft.server.EntityThrownExpBottle(world, x0, y0, z0);
            bottle.a(b0, v, b1, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)bottle;
        } else if (item.id == net.minecraft.server.Item.MONSTER_EGG.id) {
            net.minecraft.server.ItemMonsterEgg.a(world, item.getData(), x0 + b0*0.3, y0 - 0.3, z0 + b1*0.3);
            // not thrown
            entity = null;
        } else if (item.id == net.minecraft.server.Item.FIREBALL.id) {
            net.minecraft.server.EntitySmallFireball fire = new net.minecraft.server.EntitySmallFireball(world, 
                x0 + b0*0.3,
                y0,
                z0 + b1*0.3,
                b0 + random.nextGaussian() * 0.05,
                     random.nextGaussian() * 0.05,
                b1 + random.nextGaussian() * 0.05);
            entity = (net.minecraft.server.Entity)fire;
        } else {
            // non-projectile item
            net.minecraft.server.EntityItem entityItem = new net.minecraft.server.EntityItem(world, x0, y0 - 0.3d, z0, item);

            // CraftBukkit moves this code up for events.. but its only applicable here (see MCP)
            double fuzz = random.nextDouble() * 0.1 + 0.2;  // d3
            double motX = b0 * fuzz;
            double motY = v * plugin.getConfig().getDouble("dispenser.velocityItemFactor", 2.0);
            double motZ = b1 * fuzz;
            motX += random.nextGaussian() * 0.0075 * 6.0;
            motY += random.nextGaussian() * 0.0075 * 6.0;
            motZ += random.nextGaussian() * 0.0075 * 6.0;

            entityItem.motX = motX;
            entityItem.motY = motY;
            entityItem.motZ = motZ;

            entity = (net.minecraft.server.Entity)entityItem;
        }

        if (entity != null) {
            world.addEntity(entity);
        }

        world.triggerEffect(1002, x, y, z, 0);  // playAuxSfx 
        world.triggerEffect(2000, x, y, z, (int)b0 + 1 + ((int)b1 + 1) * 3);  // smoke
    }

    // Show dispenser inventory when opening adjacent crafting table
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryView view = event.getView();

        if (view.getType() != InventoryType.WORKBENCH) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof Player)) {
            plugin.log.info("inventory opened by non-player: " + holder);
            return;
        }

        Player player = (Player)holder;

        // Find clicked crafting table
        Block workbenchBlock = player.getTargetBlock(null, 100);
        if (workbenchBlock == null) {
            plugin.log.info("workbench open without target block: " + workbenchBlock);
            return;
            // we don't actually check if its a workbench.. might differ due to lag?
        }

        Block dispenserBlock = null;

        // Find adjacent dispenser
        for (BlockFace direction: surfaceDirections) {
            Block near = workbenchBlock.getRelative(direction);

            if (near.getType() == Material.DISPENSER) {
                dispenserBlock = near;
                break;
            }
        }

        if (dispenserBlock == null) {
            // just an ordinary crafting table
            return;
        }

        // Connected to a dispenser
        BlockState state = dispenserBlock.getState();
        if (!(state instanceof Dispenser)) {
            plugin.log.info("not a dispenser: " + state);
            return;
        }

        Dispenser dispenser = (Dispenser)state;

        // Copy dispenser inventory to crafting table
        // Note first slot is crafting result, which we ignore
        for (int i = 0; i < dispenser.getInventory().getSize(); i += 1) {
            inventory.setItem(i + 1, dispenser.getInventory().getItem(i));
        }

        // For tracking close
        openedCrafters.put(player, dispenser);
    }

    // Save crafting table to dispenser inventory on close
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        if (view.getType() != InventoryType.WORKBENCH) {
            return;
        }

        Inventory inventory = event.getInventory();

        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof Player)) {
            return;
        }
        Player player = (Player)holder;

        Dispenser dispenser = openedCrafters.get(player);
        if (dispenser == null) {
            return;
        }
        openedCrafters.remove(player);

        // Copy crafting table view inventory to dispenser (ignore craft result)
        for (int i = 1; i < inventory.getSize(); i += 1) {
            dispenser.getInventory().setItem(i - 1, inventory.getItem(i));
        }

        // so doesn't drop items and dupe
        inventory.clear();
        // TODO: what if someone opens dispenser and takes from it while the
        // corresponding crafting table is open??
    }
}

public class BetterDispensers extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    BetterDispensersListener listener;
    Method determineOrientationMethod;

    public void onEnable() {
        listener = new BetterDispensersListener(this);

        // Reuse piston orientation determination code since they can face in all directions
        try {
            String fieldName = getConfig().getString("determineOrientationMethod", "c"); // MCP determineOrientation

            determineOrientationMethod = net.minecraft.server.BlockPiston.class.getDeclaredMethod(
                fieldName,
                net.minecraft.server.World.class,
                int.class,
                int.class,
                int.class,
                net.minecraft.server.EntityHuman.class
                );
            determineOrientationMethod.setAccessible(true);
        } catch (Exception e) {
            log.severe("Failed to reflect, automatic orientation disabled: " + e);
            determineOrientationMethod = null;
        }

        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();
    }

    public void onDisable() {
    }

    // Get metadata for setting block orientation
    public int determineOrientation(Location loc, Player player) {
        if (determineOrientationMethod == null) {
            return -1;
        }

        int i = loc.getBlockX();
        int j = loc.getBlockY();
        int k = loc.getBlockZ();

        net.minecraft.server.EntityHuman entityhuman = ((CraftPlayer)player).getHandle();

        int l = -1;
        try {
            Object obj = determineOrientationMethod.invoke(null, new Object[] { null, i, j, k, entityhuman });
            l = ((Integer)obj).intValue();
        } catch (Exception e) {
            log.severe("Failed to invoke determineOrientation");
            return -1;
        }

        return l;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("dispenser")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            // TODO: support specifying dispensers without looking somehow
            return false;
        }

        Player player = (Player)sender;

        if (!player.hasPermission("betterdispensers.command")) {
            player.sendMessage("You do not have permission to use this command");
            return true;
        }

        Block block = player.getTargetBlock(null, getConfig().getInt("targetReach", 6));
        if (block == null || block.getType() != Material.DISPENSER) {
            sender.sendMessage("You must be looking directly at a dispenser to use this command");
            return true;
        }
        BlockState blockState = block.getState();
        Dispenser dispenser = (Dispenser)blockState;

        byte data = blockState.getRawData();

        if (args.length > 0) {
            // change direction
            try {
                data = stringToDir(args[0]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(e.getMessage());
                return true;
            }
        }

        // 0,1,6+ = no front texture, always fire west
        blockState.setData(new MaterialData(Material.DISPENSER.getId(), (byte)data));
        blockState.update(true);

        sender.sendMessage("Dispenser is facing " + dirToString(data));


        return true;
    }

    // Convert human-readable string to dispenser block data value
    static public byte stringToDir(String s) {
        char c = s.toLowerCase().charAt(0);
        switch (c) {
        case 'd': return 0;
        case 'u': return 1;
        case 'n': return 2;
        case 's': return 3;
        case 'w': return 4;
        case 'e': return 5;
        default: throw new IllegalArgumentException("Invalid direction: " + s);
        }
    }

    // Convert dispenser data value to human-readable direction string
    static public String dirToString(byte x) {
        switch (x) {
        case 0: return "down";
        case 1: return "up";
        case 2: return "north";
        case 3: return "south";
        case 4: return "west";
        case 5: return "east";
        default: return "unknown";
        }
    }

    public void log(String message) {
        if (getConfig().getBoolean("verbose", true)) {
            log.info(message);
        }
    }
}
