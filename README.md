BetterDispensers - make dispensers better

With BetterDispensers you can:

* Place dispensers facing up or down (and they shoot projectiles up/down when activated)
* Activate dispensers when they are hit by arrows (optional)
* Add arrows to dispenser inventory when hit by arrows (optional)

***[Download 1.0 here](http://dev.bukkit.org/server-mods/betterdispensers/files/1-better-dispensers-1-0/)***

BetterDispensers was inspired by 
[a post](http://www.reddit.com/r/Minecraft/comments/pp5bm/dispensers_should_be_able_to_point_straight_up/)
on /r/minecraft asking for these features. See also the
[official BetterDispensers reddit submission](http://www.reddit.com/r/Minecraft/comments/ptgv2/sure_ill_mod_that_for_you_dispensers_can_face/).

## Usage
***[Screenshots and tutorial](http://imgur.com/a/56DoO)***

1. Look down or up and place a dispenser. 
2. Add projectiles (arrows, splash potions, eggs, snowballs) to the dispenser inventory.
3. Activate the dispenser, through redstone or shooting with an arrow

When placing the dispenser, you will receive a message saying what direction it is facing.
Projectiles from the dispenser will shoot in this direction.

## Commands
"/dispenser [direction]" while looking at a dispenser lets you change or view its direction.
Requires betterdispensers.command permission.

## Permissions
betterdispensers.command (op): Allows you to use the /dispenser command

## Configuration
dispenseOnPlayerArrowHit (true): Activate dispensers when they are hit by normal player-shot or dispensed arrows.

dispenseOnOtherArrowHit (true): Activate dispensers when they are hit by other arrows, including those shot by skeletons or players with Infinity-enchanted bows.

acceptPlayerArrows (true): Add normal arrows shot by players (or dispensers) that hit dispensers to the dispenser inventory.

acceptOtherArrows (false): Add other arrows that hit dispensers to the dispenser inventory. This is false
by default since otherwise you can acquire arrows you normally cannot (those shot by skeletons or from Infinity-enchanted bows).

tellPlayer (true): Send a player a message about the dispenser orientation when placed.

verbose (false): Log debugging information to the server console.

***[Fork me on GitHub](https://github.com/mushroomhostage/BetterDispensers)***
