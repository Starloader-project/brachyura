# Starloader's Brachyura

Brachyura is a WIP build tool with a strong focus on minecraft mods. Buildscripts are written in java.
This particular fork attempts to clean up the mess of upstream, making it more pleasant to use.
Unlike upstream, this fork is specialised in general purpose modding, though it is mainly built
to power the next generation of galimulator mod loaders.

## Goals

 - Predictable
 - Simple
 - Flexible
 - Frequent nullabillity annotations
 - Good Class names

## Non-goals

 - Performance
 - Minecraft modding
 - Enterprise development

## Changes
 - A lot more nullabillity annotations
 - An alternate maven resolver which does it's best to resolve transitive dependencies
 - Merged the brachyurabootstrapconf.txt into the bootstrap jar
 - Additional dependencies can now be supplied via buildscript/build-dependencies.txt
 - CFR compiled with Java 1.8 instead of Java 1.6 so newer JDKs properly compile this project

## Community

Discord:
[![Join the Discord](https://discordapp.com/api/guilds/868569240398082068/widget.png?style=banner2)](https://discord.gg/CjnPMxsAX6)

IRC: #galimulator-modding @ irc.esper.net

## File Structure

```
.
│
├── bootstrap - Bootstrap that downloads brachyura and it's dependencies
│
├── brachyura - Source for the build tool itself
│
├── brachyura-mixin-compile-extensions - Use unknown
│
├── build - Packages the jars and creates the brachyuraboostrapconf.txt file
│
├── cfr - CFR decompiler with brachyura changes (javadocs)
│
├── crabloader - No idea
│
├── fabricmerge - Merge utilites from FabricMC Stitch seperated out and slightly improved
│
├── fernutil - Small parts of fernflower and a small library around it
│
├── javacompilelib - Simple library to compile sources using javax.tools.JavaCompiler. Supports running in a separate process and can compile with JDK 6+.
│
├── trieharder - You guessed it - I have no idea what this does
│
└── testmod - A simple test mod compiled in brachyura's junit tests
```
