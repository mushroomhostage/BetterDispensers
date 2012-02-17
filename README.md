BetterDispensers - make dispensers better

With BetterDispensers you can:

* Place dispensers facing up or down (and they shoot projectiles up/down when activated)
* Activate dispensers when they are hit by arrows (optional)
* Add arrows to dispenser inventory when hit by arrows (optional)

## Usage
1. Look down or up and place a dispenser. 
2. Add projectiles (arrows, splash potions, eggs, snowballs) to the dispenser inventory.
3. Activate the dispenser, through redstone or shooting with an arrow

The dispenser will 

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



