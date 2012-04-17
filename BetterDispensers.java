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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Random;
import java.lang.Byte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
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

class BetterDispensersAcceptTask implements Runnable {
    Arrow arrow;
    BetterDispensers plugin;

    public BetterDispensersAcceptTask(Arrow arrow, BetterDispensers plugin) {
        this.arrow = arrow;
        this.plugin = plugin;
    }

    public void run() {
        // TODO: other projectiles? snowballs?
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

        // Add arrows to inventory, if infinite/finite as configured
        net.minecraft.server.EntityArrow entityArrow = ((CraftArrow)arrow).getHandle();
        if (entityArrow.fromPlayer && plugin.getConfig().getBoolean("acceptPlayerArrows", true) ||
            !entityArrow.fromPlayer && plugin.getConfig().getBoolean("acceptOtherArrows", true)) {

            HashMap<Integer,ItemStack> excess = inventory.addItem(new ItemStack(Material.ARROW, 1));
            if (excess.size() == 0) {
                // successfully added to inventory, so remove entity
                arrow.remove();
            }
        }

        if (entityArrow.fromPlayer && plugin.getConfig().getBoolean("dispenseOnPlayerArrowHit", true) ||
            !entityArrow.fromPlayer && plugin.getConfig().getBoolean("dispenseOnOtherArrowHit", true)) {
            dispenser.dispense();
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

    public BetterDispensersListener(BetterDispensers plugin) {
        this.plugin = plugin;

        this.random = new Random();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // accept arrows inside dispensers
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow)entity;

        // must schedule a task since arrow collision detection hasn't happened yet
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new BetterDispensersAcceptTask(arrow, plugin));
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

    final int FUNCTION_CRAFTER    = 1 << 0;
    final int FUNCTION_DEPLOYER   = 1 << 1;
    final int FUNCTION_BREAKER    = 1 << 2;
    final int FUNCTION_ACTIVATOR  = 1 << 4;
    final int FUNCTION_VACUUM     = 1 << 4;
    final int FUNCTION_STORAGE    = 1 << 5;

    private int getDispenserFunctions(Block origin) {
        int functions = 0;

        BlockFace[] directions = { 
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN };

        for (BlockFace direction: directions) {
            Block near = origin.getRelative(direction);

            switch(near.getTypeId()) {
            case 58 /* crafting table */:   functions |= FUNCTION_CRAFTER; break;
            case 22 /* lapis block */:      functions |= FUNCTION_DEPLOYER; break;
            case 42 /* block of iron */:    functions |= FUNCTION_BREAKER; break;
            case 41 /* block of gold */:    functions |= FUNCTION_ACTIVATOR; break;
            case 49 /* obsidian */:         functions |= FUNCTION_VACUUM; break;
            case 54 /* chest */:            functions |= FUNCTION_STORAGE; break;
            }
        }

        return functions;
    }

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

        int functions = getDispenserFunctions(block);
        plugin.log("functions="+functions);

        boolean useStandard = false;
       
        byte data = blockState.getRawData();
        int v;
        double dy;
        switch (data) {
        case 0:     // down
            v = -1;
            dy = -1.0;
            break;
        case 1:     // up
            v = 10;
            dy = 1.0;
            break;
        case 2:     // north
        case 3:     // south
        case 4:     // west
        case 5:     // east
            // standard directions
            useStandard = true;
            //return;
        default:
            // 6-15 unused
            plugin.log("unknown data value "+data);
            return;
        }

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

            plugin.log("CRAFT: " + item);

            // Take one from all slots
            for (int i = 0; i < contents.length; i += 1) {
                tileEntity.splitStack(i, 1);
            }

        } else {
            // Take one from random slot
            int slot = tileEntity.findDispenseSlot();

            if (useStandard) {
                // TODO: do it ourselves anyways
                return;
            }

            item = tileEntity.splitStack(slot, 1);
        }
        
        // handle dispensing ourselves - see BlockDispenser.java
        event.setCancelled(true);

        // Take random item to dispense

        plugin.log("dispensing item "+item);
        if (item == null) {
            world.triggerEffect(1001, x, y, z, 0);   // "failed to dispense" effect, empty click
            return;
        }

        net.minecraft.server.Entity entity = null;

        // Create new entity at center of block face
        double x0 = x + 0.5;
        double y0 = y + 0.5 + dy;
        double z0 = z + 0.5;
    
        if (item.id == net.minecraft.server.Item.ARROW.id) {
            net.minecraft.server.EntityArrow arrow = new net.minecraft.server.EntityArrow(world, x0, y0, z0);
            arrow.shoot(0, v, 0, 1.1f, 6.0f);
            arrow.fromPlayer = true;
            entity = (net.minecraft.server.Entity)arrow;
        } else if (item.id == net.minecraft.server.Item.EGG.id) {
            net.minecraft.server.EntityEgg egg = new net.minecraft.server.EntityEgg(world, x0, y0, z0);
            egg.a(0, v, 0, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)egg;
        } else if (item.id == net.minecraft.server.Item.SNOW_BALL.id) {
            net.minecraft.server.EntitySnowball ball = new net.minecraft.server.EntitySnowball(world, x0, y0, z0);
            ball.a(0, v, 0, 1.1f, 6.0f);
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
            potion.a(0, v, 0, 1.375f, 6.0f);     // why 1.375 not 1.1? because Minecraft
            entity = (net.minecraft.server.Entity)potion;
        } else if (item.id == net.minecraft.server.Item.EXP_BOTTLE.id) {
            net.minecraft.server.EntityThrownExpBottle bottle = new net.minecraft.server.EntityThrownExpBottle(world, x0, y0, z0);
            bottle.a(0, v, 0, 1.1f, 6.0f);
            entity = (net.minecraft.server.Entity)bottle;
        } else if (item.id == net.minecraft.server.Item.MONSTER_EGG.id) {
            net.minecraft.server.ItemMonsterEgg.a(world, item.getData(), x0, y0, z0);
            // not thrown
            entity = null;
        } else if (item.id == net.minecraft.server.Item.FIREBALL.id) {
            // TODO: doesn't seem to work right.. figure out the other parameters
            net.minecraft.server.EntitySmallFireball fire = new net.minecraft.server.EntitySmallFireball(world, x0, y0, z0, 0, 0, 0);
            entity = (net.minecraft.server.Entity)fire;
        } else {
            // non-projectile item
            net.minecraft.server.EntityItem entityItem = new net.minecraft.server.EntityItem(world, x0, y0 - 0.3d, z0, item);

            double d0 = (double)x + 0.5d;
            double d1 = (double)y + 0.5d;
            double d2 = (double)z + 0.5d;
            double d3 = random.nextDouble() * 0.1d + 0.2d;
            double motX = d3;
            double motY = 0.2d;
            double motZ = d3;

            motX += random.nextGaussian() * 0.0075 * 6.0d;
            motY += random.nextGaussian() * 0.0075 * 6.0d;
            motZ += random.nextGaussian() * 0.0075 * 6.0d;

            entityItem.motX = motX;
            entityItem.motY = motY;
            entityItem.motZ = motZ;

            entity = (net.minecraft.server.Entity)entityItem;
        }

        if (entity != null) {
            world.addEntity(entity);
        }

        world.triggerEffect(1002, x, y, z, 0);  // playAuxSfx 
        world.triggerEffect(2000, x, y, z, 4);  // smoke
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
