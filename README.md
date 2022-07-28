# orion
A Diplomatic Way of Designing and Implementing Asynchronous/Self-Timed Circuits


## TODO
- [ ] Clean up some of the type inferencing. Need to see how we can make some of this easier
- [ ] Create verilog blocks on the fly based on the data type (i.e. iterate over bundle elements)
    * This will cause us to change the `dataInit` for various components (e.g.`DecoupledReg`) to `UInt`
- [ ] .lib parsing with config for capturing semi-accurate timing information based on process
- [ ] See about resolving circular dependencies with node graphs. This will likely require a diplo rework or extension