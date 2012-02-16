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

class AntiDispenserTask implements Runnable {
    Arrow arrow;
    AntiDispenser plugin;

    public AntiDispenserTask(Arrow arrow, AntiDispenser plugin) {
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
        /* 0,1,6+ = no front texture, always fire west
        blockState.setData(new MaterialData(Material.DISPENSER.getId(), (byte)1));
        blockState.update(true);
        */

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

class AntiDispenserListener implements Listener {
    AntiDispenser plugin;

    public AntiDispenserListener(AntiDispenser plugin) {
        this.plugin = plugin;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow)entity;

        // must schedule a task since arrow collision detection hasn't happened yet
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new AntiDispenserTask(arrow, plugin));
    }

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

        // shoot arrows outselves
        event.setCancelled(true);

        net.minecraft.server.World world = ((CraftWorld)block.getWorld()).getHandle();

        net.minecraft.server.EntityArrow arrow = new net.minecraft.server.EntityArrow(
            world,
            block.getX() + 0.5,
            block.getY() + 0.5 + 2.0,
            block.getZ() + 0.5);
        arrow.shoot(0, 0.10000000149011612D*10, 0, 1.1f, 6.0f);
        arrow.fromPlayer = true;
        world.addEntity(arrow);

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
}
