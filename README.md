# Iris

The fork is only for Minecraft `1.16.5`.  
Based on `iris-1.8.7`.

MythicMobs 4.x supported.

# [Documentation](https://docs.volmit.com/iris/) **|** [Dumension Packs](https://github.com/IrisDimensions)

Default `overworld` pack for this fork is now available in [releases](https://github.com/MrXiaoM/Iris/releases/tag/overworld).

## Iris Toolbelt

Everyone needs a toolbelt.

```java
package com.volmit.iris.core.tools

// Get IrisDataManager from a world
IrisToolbelt.access(anyWorld).getCompound().getData();

// Get Default Engine from world
IrisToolbelt.access(anyWorld).getCompound().getDefaultEngine();

// Get the engine at the given height
IrisToolbelt.access(anyWorld).getCompound().getEngineForHeight(68);

// IS THIS THING ON?
boolean yes=IrisToolbelt.isIrisWorld(world);

// GTFO for worlds (moves players to any other world, just not this one)
IrisToolbelt.evacuate(world);

IrisAccess access=IrisToolbelt.createWorld() // If you like builders...
  .name("myWorld") // The world name
  .dimension("terrifyinghands")
  .seed(69133742) // The world seed
  .headless(true)  // Headless make gen go fast
  .pregen(PregenTask // Define a pregen job to run
  .builder()
    .center(new Position2(0,0)) // REGION coords (1 region = 32x32 chunks)
    .radius(4)  // Radius in REGIONS. Rad of 4 means a 9x9 Region map.
    .build())
  .create();
```
