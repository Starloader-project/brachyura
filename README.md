Jetbrains runtime PSA: The jetbrains runtime is known to cause test failures while compiling slbrachyura.
Either use a different JDK for compilation or use `build.bash` and then
`./offline-build.bash` for compilation. However the Jetbrains runtime will NOT be able to run
brachyura for most minecraft-related projects, will work fine with galimulator however due to
starplane using sl-deobf for remapping and thus it does not use Tiny-Remapper where the error
occurs.

# Starloader's Brachyura

Brachyura is a WIP build tool with a strong focus on minecraft mods. Buildscripts are written in java.
This particular fork attempts to clean up the mess of upstream, making it more pleasant to use.
Unlike upstream, this fork is specialised in general purpose modding, though it is mainly built
to power the next generation of galimulator mod loaders - minecraft modding will still be supported
nonetheless.

## Goals

 - Predictable
 - Simple
 - Flexible
 - Frequent nullabillity or contract annotations
 - Good Class names
 - Ease of use

## Non-goals

 - Performance
 - Enterprise development

## Changes
 - A lot more nullabillity annotations
 - An alternate maven resolver which does it's best to resolve transitive dependencies
 - Merged the brachyurabootstrapconf.txt into the bootstrap jar
 - Additional dependencies can now be supplied via buildscript/build-dependencies.txt
 - CFR compiled with Java 1.8 instead of Java 1.6 so newer JDKs properly compile this project
 - Abillity to name buildscript projects
 - Eclipse External Annotations and IntelliJ Annotations support
 - "createTemplate" argument, which creates a small template project to get the ball rolling
 - API for passing arguments more or less directly to the compiler (however not honored in the fabric project right now)
 - XDG_DATA_HOME is now honored by default (Resolves upstream's https://github.com/CoolCrabs/brachyura/issues/8)
 - Snapshot repository support

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
