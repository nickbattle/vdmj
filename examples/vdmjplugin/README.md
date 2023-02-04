## Example VDMJ Plugin

This project contains a very simple example of how to write a VDMJ plugin. The example adds itself
to the list of plugins that intercept the CheckSyntaxEvent and adds errors and warnings if the
names of functions and operations (any dialect) do not follow a particular pattern or are too long.

The plugin is ENABLED by a "-check <max>" command line option, which sets the name length beyond
which a warning will be issued. Names which do not conform to the upper/lower case pattern Xxxx...
are treated as errors. Obviously this is just an example :-)

The plugin also adds a "maxlen" console command, which allows you to change the maxlen and re-run the
checks. This option is added to the "help" output for the console.

To use the plugin, start VDMJ using the main class com.fujitsu.vdmj.plugins.VDMJ, and set the Java
property: -Dvdmj.plugins=examples.vdmjplugin.ExamplePlugin
