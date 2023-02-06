## Example RemoteSimulation

This project contains a very simple example of how to write a RemoteSimulation. As the comments in
the source say:

```
 * This simple example is designed to run with the sporadic.vdmrt test spec in
 * src/test/resources. It demonstrates setting parameters and changing the value
 * of runtime system variables during a simulation.
 * 
 * Execute in VDM by adding the project to the classpath and passing:
 * 
 *     -simulation simulation.TestSimulation
 * 
 * Then execute new Test().test() as usual. The output should be like:
 * 
 * > p new Test().test()
 * Last = 0
 * Updated = 1
 * Last = 1
 * Updated = 2
 * Last = 2
 * Updated = 3
 * Last = 3
 * Updated = 4
 * Last = 4
 * Updated = 5
 * Last = 5
 * Updated = 6
 * Last = 6
 * Updated = 7
 * = ()
 * Executed in 0.057 secs.
```

