BetterDispensers - vertical dispensers, automatic crafting, block breakers, conduits, and more!

Dispensers, reinvented. Featuring:

* Vertical dispensers
* TNT, liquid, boat, minecart dispensing
* Conduits for transporting items to and from dispensers
* New dispenser functions: crafter, interactor, breaker, vacuum, accelerator, storage, filler, turret
* New dispenser functions can be combined with each other
* No client mods required
* Highly configurable

**[Download BetterDispensers 2.1](http://dev.bukkit.org/server-mods/betterdispensers/files/7-better-dispensers-2-1/)** - released 2012/05/19 for 1.2.5

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
* Reaches up to 7 blocks in a straight line from the dispenser
* Sends a BlockBreakEvent to other plugins (with user "[BetterDispensers]") and respects modifications
* Modified tools from plugins like [EnchantMore](http://dev.bukkit.org/server-mods/enchantmore/) are supported (e.g., Shovel + Flame on sand = glass; drops only)
* Bedrock and portal blocks cannot be broken
* Useful for making an automated tree farm, automated cobble generator, etc.
* Will not dispense if no tool is present or block cannot be broken
* Works great when combined with *Filler*

### Vacuum

The **vacuum** is created by placing an *obsidian* block next a dispenser.

* When activated, sucks up item drops within 8 blocks into the dispenser inventory before dispensing
* Items dropped from players automatically vacuumed up within 2 blocks, no need to activate
* Player arrows hitting the dispenser will be added as well
* Arrows hitting the dispenser from skeletons will not be added by default, but can be enabled
* *Important*: will not vacuum if the dispenser is empty - fill it with something

### Accelerator

The **accelerator** is created by placing a *gold block* next to a dispenser.

* Doubles Y velocity of dispensed item

### Storage

Any container block placed either directly adjacent to a dispenser, or at the end
of a glass conduit connected to the dispenser, will augment its **storage** capabilities.
The dispenser inventory specifies what items to take, and still must be filled.

* A random item will be first chosen from *dispenser* in all cases
* If the other container has an item of the same type, it will be taken from the container instead of the dispenser
* If the other container does not have the item, it will be taken from the dispenser as usual
* If the container is empty, the next one (if any) along the conduit will be chosen
* Containers include: chests, furnaces, brewing stands, other dispensers, custom container types (including [Iron Chests](http://www.minecraftforum.net/topic/981855-125-forge-sspsmpbukkit-ironchests-331/))
* *Important*: will not dispense if the dispenser is empty - fill it with what you want to pull

### Conduit

**Conduits** form BetterDispenser's primitive transport networks. Built out of
*glass*, they can stretch up to 1000 contiguous blocks and can instantly move items between
dispensers and other containers.

* When directly connected to a dispenser, the dispenser pulls items *out* of the container at the end of the conduit (see *Storage*)
* When connected via wooden plank to a dispenser, the dispenser dispenses items *into* the conduit (see *Filler*)
* Only can follow one route

### Filler

The **filler** is created by placing a *wooden plank* next to a dispenser, optionally connected to a conduit.
They override dispenser's normal dispensing as items in the world, and instead dispense items into a conduit.

* Fillers insert items into a conduit, taken from the dispenser
* If the end of the conduit is connected to a container, items will be placed within it
* If not, items will be dropped on the ground at the end of the conduit
* If the container overflows, excess items will be dropped on the ground

### Turret

The **turret** is created by placing a **brick** block next to a dispenser. 

* After each dispense, the orientation will rotate
* Try it with TNT

## Permissions
betterdispensers.command (op): Allows you to use the /dispenser command

## Configuration
Features can be turned off or tweaked as desired. Default configuration:

    verbose: false                  # log debugging information to the server console
    tellPlayer: true                # send a player a message about the dispenser orientation when placed

    dispenser:
        sneakReverseOrientation: true   # shift-click when placing dispenser to orient away from you
        overrideHorizontal: true    # handle horizontal dispensing ourselves, disable to defer to Minecraft)
        overrideVertical: true      # handle vertical dispensing
        velocityHorizontal: 0.1     # small default velocity
        velocityDown: -0.05         # velocity when dispensing downward
        velocityUp: 0.4             # larger velocity when dispensing upward
        dispenseOnPlayerArrows: true    # when hit by player arrows, activate dispenser
        dispenseOnNonPlayerArrows: true # when hit by skeleton arrows, activate dispenser
        arrowEnable: true           # shoot arrows
        arrowForce: 1.1             # force of shot arrows
        arrowSpread: 6.0            # random variation; set to 0 for precision
        eggEnable: true             # throw eggs
        eggForce: 1.1
        eggSpread: 6.0
        snowballForce: 1.1          # throw snowballs
        snowballSpread: 6.0
        snowballEnable: true
        potionEnable: true          # throw splash potions
        potionForce: 1.375          # more forceful in vanilla for some reason
        potionSpread: 6.0
        expbottleEnable: true       # throw experience bottles
        expbottleForce: 1.1
        expbottleSpread: 6.0
        spawnEggEnable: true        # hatch spawn eggs
        fireballEnable: true        # ignite fire charges
        fireballRandomMotionX: 0.05
        fireballRandomMotionY: 0.05
        fireballRandomMotionZ: 0.05
        tntEnable: true             # prime TNT
        tntVelocityFactorY: 3.0     # multiply Y velocity for dispensing primed TNT
        tntVelocityFactorHorizontal: 5.0    # multiple X and Z velocity
        tntFuzz: 0.1                # random X/Y motion Gaussian maximum, less than item
        tntVelocityBaseX: 1.0       # fixed velocity offset
        tntVelocityBaseY: 0.0
        tntVelocityBaseZ: 1.0
        tntRandomMotionX: 0.045     # additional random Gaussian motion
        tntRandomMotionY: 0.045
        tntRandomMotionZ: 0.045
        tntFuseTicks: 15            # time in ticks before exploding
        liquidsEnable: true         # dispense liquid _blocks_ (or any block)
        liquids:                    # liquids to flow from dispenser (blocks, not buckets)
        - 8     # water source
        - 9     # water flow
        - 10    # lava source
        - 11    # lava flow
        - 162   # Buildcraft oil source
        - 163   # Buildcraft oil flow
        bucketsEnable: true         # empty liquids from buckets
        bucketsKeep: true           # keep the empty bucket, rather than removing it
        buckets:
        - 326   # water bucket
        - 327   # lava bucket
        - 4063  # Buildcraft oil bucket
        bucketLiquids:
          326: 8    # water bucket -> water source
          327: 10   # lava bucket -> lava source
          4063: 162 # Buildcraft oil bucket -> oil source
        boatEnable: true            # drop boats
        cartEnable: true            # drop minecarts
        itemEnable: true            # all other items dispense as item drops
        itemVelocityFactorY: 2.0    # multiply Y velocity for dispensing non-projectile items (Minecraft default)
        itemFuzz: 0.3               # random X/Y motion Gaussian maximum
        itemRandomMotionX: 0.045    # additional random Gaussian motion
        itemRandomMotionY: 0.045
        itemRandomMotionZ: 0.045


    crafter:
        enable: true
        blockID: 58     # crafting table

    interactor:
        enable: true
        blockID: 22     # lapis block
        reachLimit: 7

    breaker:
        enable: true
        blockID: 42     # iron block
        reachLimit: 7
        unbreakableBlockIDs: 
        - 7             # bedrock
        - 90            # nether portal
        - 119           # end portal
        - 120           # end portal frame

    vacuum:
        enable: true
        blockID: 49     # obsidian
        enablePlayerArrows: true        # accept arrows into dispenser if hit, from player or dispenser
        enableNonPlayerArrows: false    # accept arrows from skeletons or infinity bows (see also: http://dev.bukkit.org/server-mods/pickuparrows/)
        reachLimit: 8.0                 # before dispensing, vacuum up entities with this distance
        itemDropRange: 2                # vacuum up player item drops within this many blocks
        itemDropDelayTicks: 10          # delay before vacuuming player item drops

    accelerator:
        enable: true
        blockID: 41     # gold block
        velocityFactorY: 2.0            # multiply Y velocity when accelerated

    storage:
        # any container block

    conduit:
        blockID: 20     # glass
        enableDirectConnection: true    # glass can connect directly to dispensers to pull from storage (vs through fillers)
        maxLength: 1000

    filler:
        enable: true
        blockID: 5      # plank
        unconnectedDrop: true       # if left unconnected, drop items on ground, otherwise discard
        overflowDrop: true          # if destination overflows, drop items on ground, otherwise discard

    turret:
        enable: true
        blockID: 45     # bricks




## See also

* [Buildcraft](http://www.mod-buildcraft.com/) - client/server mod with pipes, automatic crafting, mining
* [RedPower](http://www.minecraftforum.net/topic/365357-125-eloraams-mods-redpower-2-prerelease-5/) - client/server mod with pneumatic tubes, block breaker, deployer, project table
* [MineFactory](https://github.com/balr0g/MineFactoryReloaded/wiki) - client/server mod with conveyor belts
* [MachinaCraft](http://dev.bukkit.org/server-mods/machinacraft) - server plugin framework, [MachinaFactory](http://dev.bukkit.org/server-mods/machinacraft/pages/machina-factory/) module includes pipelines, fabricator, item relays
* [Plugin Request: Buildcraft for bukkit?](http://forums.bukkit.org/threads/buildcraft-for-bukkit.21393/#post-475948) - idea of transferring items through glass
* [Sure, I'll mod that for you: dispensers can face upward and downward! For shooting arrows, splash potions, snowballs, or even eggs (perfect for traps). Also: dispensers accept arrows and dispense when hit (BetterDispensers v1.0)](http://www.reddit.com/r/Minecraft/comments/ptgv2/sure_ill_mod_that_for_you_dispensers_can_face/). - initial release
* [Request: Dispensers should be able to point straight up and straight down.](http://www.reddit.com/r/Minecraft/comments/pp5bm/dispensers_should_be_able_to_point_straight_up/) - initial inspiration


***[Fork me on GitHub](https://github.com/mushroomhostage/BetterDispensers)***
