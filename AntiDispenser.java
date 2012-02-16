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

package me.exphc.AntiDispenser;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.Formatter;
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
import org.bukkit.craftbukkit.CraftWorld;

class AntiDispenserAcceptTask implements Runnable {
    Arrow arrow;
    AntiDispenser plugin;

    public AntiDispenserAcceptTask(Arrow arrow, AntiDispenser plugin) {
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

        arrow.remove();

        // TODO: only if not infinite arrow?
        inventory.addItem(new ItemStack(Material.ARROW, 1));
        // TODO: detect if full! then don't remove entity

        // TODO: configurable
        if (plugin.getConfig().getBoolean("dispense", true)) {
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
            plugin.log.info("getArrowHit("+arrow+" reflection failed: "+e);
            throw new IllegalArgumentException(e);
        }
    }

}

// Task to set dispenser orientation
class AntiDispenserOrientTask implements Runnable {
    byte data;
    Block block;
    AntiDispenser plugin;

    public AntiDispenserOrientTask(byte data, Block block, AntiDispenser plugin) {
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

class AntiDispenserListener implements Listener {
    AntiDispenser plugin;

    public AntiDispenserListener(AntiDispenser plugin) {
        this.plugin = plugin;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // accept arrows inside dispensers
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow)entity;

        // must schedule a task since arrow collision detection hasn't happened yet
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new AntiDispenserAcceptTask(arrow, plugin));
    }

    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER) {
            return;
        }

        Block blockAgainst = event.getBlockAgainst();
        Player player = event.getPlayer();

        plugin.log.info("placed "+block.getLocation()+" by "+player.getLocation());

        Location from = player.getLocation();
        Location to = block.getLocation();

        double distance = from.distance(to);

        int dy = from.getBlockX() - to.getBlockX(); // + if we're above, - if below
        player.sendMessage("dy="+dy+", distance="+distance);
        // TODO: we need to intelligent set orientation, like how pistons do
        byte data = (byte)1;

        //TODO Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new AntiDispenserOrientTask(data, block, plugin));
    }

    // handle up/down dispensers
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockDispense(BlockDispenseEvent event) {
        plugin.log.info("dispense"+event);
        Block block = event.getBlock();
        BlockState blockState = block.getState();
        if (!(blockState instanceof Dispenser)) {
            return;
        }
        
        Dispenser dispenser = (Dispenser)blockState;
       
        // velocity from event doesn't change arrows
        // see BlockDispenser.java, special cases for projectiles, do not take into account motX/Y/Z
        //event.setVelocity(new Vector(0, 1, 0));
        //event.setItem(new ItemStack(Material.SNOW, 1)); 
        /*
        ItemStack item = event.getItem();
        if (item.getType() != Material.ARROW) {
            return;
        }*/

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
            return;
        default:
            // 6-15 unused
            plugin.log.info("unknown data value "+data);
            return;
        }

        // shoot arrows outselves
        event.setCancelled(true);
        // TODO: we still need to remove arrow from dispenser inventory!
        // otherwise we dupe arrows

        net.minecraft.server.World world = ((CraftWorld)block.getWorld()).getHandle();
        int x = block.getX(), y = block.getY(), z = block.getZ();

        net.minecraft.server.EntityArrow arrow = new net.minecraft.server.EntityArrow(
            world,
            x + 0.5,        // center of block face
            y + 0.5 + dy,
            z + 0.5);
        arrow.shoot(0, v, 0, 1.1f, 6.0f);   // up
        arrow.fromPlayer = true;
        world.addEntity(arrow);
        world.f(1002, x, y, z, 0);  // playAuxSfx - dispenser smoke

        /*
        block.getRelative(0,1,0).setType(Material.GLASS);
        block.getWorld().spawnArrow(block.getRelative(0,2,0).getLocation(), new Vector(0,1,0), 10, 0);
        */
    }
}

public class AntiDispenser extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    AntiDispenserListener listener;

    public void onEnable() {
        listener = new AntiDispenserListener(this);
    }

    public void onDisable() {
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
            data = stringToDir(args[0]);
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
}
