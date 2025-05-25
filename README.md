# Improved Maps [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This mod is only for Fabric Servers(or Single-Player) and requires [Fabric API](https://modrinth.com/mod/fabric-api). Works with Vanilla clients thanks to [Polymer](https://modrinth.com/mod/polymer)!

### Current Features
- **Atlas** - A combination between a bundle and a map. 
  - Fill it with empty maps and it will automatically create new maps as you explore off the edge of your current map. 
  - Automatically switch to the relevant map when returning to previous areas.
  - Make a copy of the entire Atlas. Great for map walls!
  - Fits up to 512 filled maps, and any number of empty maps
- **Map Information Tooltips** - See the map scale and center coordinates right from the tooltip

### How to use Atlases
- Craft one by combining a filled map and a book in a crafting bench
  - An Atlas is specific to a dimension
  - All maps in an Atlas must have the same scale
- Atlases behave like bundles, but can only take filled and empty maps
- Copy an Atlas by combining an Atlas and a book, either in a crafting bench or a cartography table
  - You must have enough empty maps in the Atlas to copy it, otherwise it won't craft
  - You'll get 2 identical Atlases, each containing half the remaining empty maps
- You can take maps out like you would take items out of a bundle. To take out the empty maps you can either place the Atlas in a cartography table, or take out all the filled maps then right-click the empty Atlas in your inventory

### (Optional) Client-side features
- Installing the mod client-side will show custom icons for the Atlas item and fix some UI oddities
- Icons can also be used by providing the Polymer resource pack to your vanilla clients
  - Find out how to setup Polymer AutoHost here: https://polymer.pb4.eu/latest/user/resource-pack-hosting/
  - Run `/polymer generate-pack` after installing the mod to generate the resource pack

### Known Issues
- Cartography table result slot doesn't work well. You can click the empty slot and get the correct result, it just won't preview
  - Installing the mod client-side will fix this
- Bundle tooltip on Atlas will show full once it hits 64 maps. You can still keep adding maps
  - Installing the mod client-side will fix this

### Shout-outs
- [Pillowsledder](https://bsky.app/profile/pillowsledder.bsky.social) for creating the excellent icons!
- [Map Atlases](https://modrinth.com/mod/map-atlases) by Pepperoni-Jabroni for the original idea and map switching logic.
- [Diversity: Better Bundle](https://modrinth.com/mod/diversity-better-bundle) by FaeWulf for implementation of larger bundle sizes
