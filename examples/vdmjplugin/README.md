## Example VDMJ Plugin

This project contains a very simple example of how to write a VDMJ plugin. The example adds itself
to the list of plugins that intercept the CheckSyntaxEvent and adds errors and warnings if the
names of functions and operations (any dialect) do not follow a particular pattern or are too long.

The plugin is enabled by a "-check <max>" command line option, which sets the name length beyond
which a warning will be issued. Names which do not conform to the upper/lower case pattern Xxxx...
are treated as errors. Obviously this is just an example :-)

To use the plugin, start VDMJ using the main class com.fujitsu.vdmj.plugins.VDMJ, and set the Java
property: -Dvdmj.plugins=examples.vdmjplugin.ExamplePlugin
