# orion
A Diplomatic Infrastructure for Designing and Implementing Asynchronous/Self-Timed Circuits

The [Orion Project](https://www.halopedia.org/ORION_Project) is an effort to ease some of the challenges
with selftimed circuits by providing a layer of abstraction, allowing a designer to focus more on the architecture 
versus low level verilog implementations.

The backbone of Orion is based on [Diplomacy](https://github.com/chipsalliance/diplomacy) to aid in component connections (req/ack, assertions, etc.) as well
as future planned expansions for process specific performance estimates, SDC generation, and token based optimizations.

Orion is heavily based on [Click Elements](https://ieeexplore.ieee.org/document/5476997) particularly the phase-decoupled
click-based components explained and developed by Jens Sparso and team (https://github.com/zuzkajelcicova/Async-Click-Library).
A good source of information is also Jens Sparso's [Introduction to Asynchronous Circuit Design](https://backend.orbit.dtu.dk/ws/portalfiles/portal/215895041/JSPA_async_book_2020_PDF.pdf).

While Orion is based on Click Elements, a long term goal is to be able to switch out the component types to target
different architectures (e.g. Moustrap, Loihi, generic C-Elements). As the project grows, this will be evaluated for the best way to
move forward if the desire is there for a feature like this, more than likely resulting in a rearchitected revision of the project.

## Background
While self-timed circuits is nothing new, it hasn't seemed to get as much attention as globally synchronous
designs. This is understandable due to the nature of complexity with regards to self-timed circuits
and how EDA has generally favored synchronous designs. Chisel and Diplomacy offered a new way to look at
hardware design, and while even Chisel was originally developed with just synchronous designs in mind, the 
infrastructure had the ability to create a library that could potentially be used for self-timed designs.

It would be naive to think that this infrastructure will be the go-to self-timed circuit generation framework, however
hopefully this allows some exploration that previously seemed too difficult. In addition, the framework shows how
the industry can use the higher level libraries such as Chisel to achieve more than we could with traditional Verilog
designs.

## TODO
- [ ] Clean up some of the type inferencing. Need to see how we can make some of this easier
- [ ] .lib parsing with config for capturing semi-accurate timing information based on process
- [ ] See about resolving circular dependencies with node graphs. This will likely require a diplo rework or extension
- [ ] See about updating to [CHIPS Diplomacy](https://github.com/chipsalliance/diplomacy)