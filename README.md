In the eternal search for the ideal build tool for galimulator, many build system were tried out.
Initially gradle seemed like a good way of building things, but in the end it was very tedious
to work with. So naturally, I searched for alternatives and ended up stumbling over brachyura,
which did the job well enough, but had to be improved in order to do the job in a great way.
These improvements ended up in this little repository which became known as slbrachyura. However
with time upstream became more distanced from it's former self that gave it it's greatness.
As such I (Geolykt) would always be faced with the decision of whether to hard-fork from brachyura,
which would mean that the project can be developed to my own constraints (modding galimulator
is a bit different to modding minecraft, as it seems like) or to keep pulling from upstream
and kindof ignoring the regressions - Mind that you cannot really have the best of both worlds
without spending enormous amount of time to resolve merge conflicts.

After some more experimentation it became increasingly more clear that brachyura's task system
has it's flaws that can only be remedied by rewriting large parts of it. Slbrachyura was able
to perform the rewrite halfway and it is technically possible to complete it from my analysis,
but again that would mean a lot of work that should really be put into more productive things.
Another inherent issue is that brachyura is quite bloated if one wants to only mod galimulator
with it, and important features such as maven resolving or plugins are missing.

Other findings were that buildscripts written in java are simply not necessary most of the time,
and usually pre-compiled plugins suffice. Brachyura however does not support plugins in a way
that is modular. I am quite certain that this lack of modularity can't easily be resolved
within brachyura and providing the modularity without it becoming a mess would amount nothing
short of a rewrite. So in the end I am most likely doing exactly that whith intial skeletons
showing that a hyper-modular build system can indeed work.

The Starloader-API is going to continue to use slbrachyura alongside sl-starplane, but **that
won't be forever.** However the Starloader-API will continue to sl-starplane for the forseeable
future as the next generations of build systems for galimulator mods will use an entirely
new architecture (mmStarmap) and migrating Starloader-API from the old architecture (spStarmap &
slIntermediary) to the new one will take a considerable time. Until then slbrachyura will still
be maintained, after that maintainance on slbrachyura will be based on whether it is needed.

In short: **If I am not aware of anyone using slbrachyura, it will get retired.** On the flip side,
if people still use it and I am aware of that, slbrachyura will stay maintained.

---

# Starloader's Brachyura (Slbrachyura)

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
 - Improved task system

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
