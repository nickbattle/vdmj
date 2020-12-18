## The V2C Translation Example

This project contains a very simple example of how to use both VDMJ command-line plugins
and the VDMJ analysis tree-creation system.

The command-line plugin is very simple and in the "plugins" package of the project. You
have to create plugins in this package, unless you set the "vdmj.plugins" property to something
else. There are more comments in the [TranslatePlugin](src/main/java/plugins/TranslatePlugin.java)
class describing its actions. But to use the plugin, you just need to add this v2c project to the classpath.
When you type "translate" in the command-line, the TranslatePlugin will be found, loaded, and passed the
command line arguments, if any. As you'll see from the class, this creates a new "TR" tree of objects from
the TC (type checker) tree using VDMJ's ClassMapper, and then it uses the new tree to translate
the specification into "C"... or at least very very simple specifications! Remember, this is
an example :-)

The tree-creation process starts [tc-tr.mappings](src/main/resources/tc-tr.mappings) file. This
defines how to translate each of the (relevant) TC tree classes into TR classes. The mapping
file has more comments on the file format and the settings used.