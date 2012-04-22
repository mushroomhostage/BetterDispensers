BetterDispensers - vertical dispensers, automatic crafting, block breakers, conduits, and more!

Features:

* Place dispensers facing up or down
* No client mods required
* Highly configurable

With BetterDispensers you can:

* Place dispensers facing up or down (and they shoot projectiles up/down when activated)
* Activate dispensers when they are hit by arrows (optional)
* Add arrows to dispenser inventory when hit by arrows (optional)

## Basic Usage

First and foremost, BetterDispensers lets you orient dispensers vertically:

***[Screenshots and tutorial](http://imgur.com/a/56DoO)***

1. Look down or up and place a dispenser
2. Add items (including projectiles) to the dispenser inventory
3. Activate the dispenser

When placing the dispenser, you will receive a message saying what direction it is facing.
Items dispensed from the dispenser will shoot in this direction.

[Plugin Showcase video from WoopaGaming (v1.1)](http://www.youtube.com/watch?v=ZkNV41VP9T4)

But that's just the beginning.

## Advanced Usage

Regular dispensers have been enhanced:

* Use the "/dispenser [direction]" while looking at a dispenser to change or view its direction
* Hold shift while placing a dispenser to face it away from you
* Add TNT, it will be primed 
* Add liquids or buckets, they will be emptied out
* Add boats or minecarts, they will be placed
* Configure any of the velocities, force, spreads, allowed projectiles etc. in config.yml

New kinds of dispenser can be created by placing specific blocks directly adjacent.
Multiple blocks can be placed to combine their functionality. The new dispensers are:

### Crafter

The **crafter** is created by placing a *crafting table* next to a dispenser. 
It is both an automatic crafting table and persistent crafting table.

* You can access the dispenser's inventory through the crafting table
* The crafting table inventory persists, no more items falling on the floor
* When activated, dispenses items crafted from its internal crafting grid
* Will not dispense if the recipe is invalid
* Works great when combined with *Storage*

### Interactor

The **interactor** is created by placing a *lapis lazuli block* next to a dispenser.

* When activated, will "use" the item on a block
* Plant saplings, grow crops with bonemeal, till dirt with hoes, light fire with flint & steel, place blocks, etc.
* Helpful to automate your farming
* Acts as if you right-clicked the item on the top of the block
* Reaches up to 7 blocks, staggering directly in front of the dispenser and one block below
* Uses up tool durability
* Will not dispense if item cannot interact with the block

### Breaker

The **breaker** is created by placing an *iron block* next to a dispenser.

* When activated, breaks blocks and dispenses its drops
* Requires a tool in the dispenser, and its durability will be used up
* Blocks are broken instantly
* Item drops take into account the tool used
* Reaches up to 7 blocks in a straight line from the dispenser
* Sends a BlockBreakEvent to other plugins (with user "[BetterDispensers]") and respects modifications
* Modified tools from plugins like [EnchantMore](http://dev.bukkit.org/server-mods/enchantmore/) are supported (e.g., Shovel + Flame on sand; drops only)
* Bedrock and portal blocks cannot be broken
* Useful for making an automated tree farm, automated cobble generator, etc.
* Will not dispense if no tool is present or block cannot be broken
* Works great when combined with *Filler*

### Vacuum

The **vacuum** is created by placing an *obsidian* block next a dispenser.

* When activated, sucks up item drops within 8 blocks into the dispenser inventory before dispensing
* Items dropped from players automatically vacuumed up within 2 blocks, no need to activate
* Player arrows hitting the dispenser will be added as well
* Arrows hitting the dispenser from skeletons will not be added by default, but can be changed

### Accelerator

The **accelerator** is created by placing a *gold block* next to a dispenser.

* Doubles Y velocity of dispensed item

### Storage

Any container block placed either directly adjacent to a dispenser, or at the end
of a glass conduit, will augment its **storage** capabilities.
The dispenser inventory specifies what items to take, and still must be filled.

* A random item will be first chosen from *dispenser* in all cases
* If the other container has an item of the same type, it will be taken from the container instead of the dispenser
* If the other container does not have the item, it will be taken from the dispenser as usual
* Containers include: chests, furnaces, brewing stands, other dispensers, custom container types (including [Iron Chests](http://www.minecraftforum.net/topic/981855-125-forge-sspsmpbukkit-ironchests-331/))
* Will not dispense if the dispenser is empty

### Conduit

**Conduits** form BetterDispenser's primitive transport networks. Built out of
*glass*, they can stretch up to 100 contiguous blocks and can instantly move items between
dispensers and other containers.

* When directly connected to a dispenser, the dispenser pulls items out of the container at the end of the conduit, if any (see *Storage*)
* When connected via wooden plank to a dispenser, the dispenser dispenses items *into* the conduit (see *Filler*)

## Filler

The **filler** is created by placing a *wooden plank* next to a dispenser, followed by a conduit.

## Permissions
betterdispensers.command (op): Allows you to use the /dispenser command

## Configuration
Features can be turned off or tweaked as desired. Default configuration:


## See also

* [Buildcraft](http://www.mod-buildcraft.com/) - client/server mod with pipes, automatic crafting, mining
* [RedPower](http://www.minecraftforum.net/topic/365357-125-eloraams-mods-redpower-2-prerelease-5/) - client/server mod with pneumatic tubes, block breaker, deployer, project table
* [MineFactory](https://github.com/balr0g/MineFactoryReloaded/wiki) - client/server mod with conveyer belts
* [MachinaCraft](http://dev.bukkit.org/server-mods/machinacraft) - server plugin framework, [MachinaFactory](http://dev.bukkit.org/server-mods/machinacraft/pages/machina-factory/) module includes pipelines, fabricator, item relays
* [Plugin Request: Buildcraft for bukkit?](http://forums.bukkit.org/threads/buildcraft-for-bukkit.21393/#post-475948) - idea of transferring items through glass
* [Sure, I'll mod that for you: dispensers can face upward and downward! For shooting arrows, splash potions, snowballs, or even eggs (perfect for traps). Also: dispensers accept arrows and dispense when hit (BetterDispensers v1.0)](http://www.reddit.com/r/Minecraft/comments/ptgv2/sure_ill_mod_that_for_you_dispensers_can_face/). - initial release
* [Request: Dispensers should be able to point straight up and straight down.](http://www.reddit.com/r/Minecraft/comments/pp5bm/dispensers_should_be_able_to_point_straight_up/) - initial inspiration


***[Fork me on GitHub](https://github.com/mushroomhostage/BetterDispensers)***
