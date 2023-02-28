## The V2C Translation Example

This project contains a very simple example of how to use both VDMJ plugins and the VDMJ analysis
tree-creation system (the ClassMapper).

The VDMJ plugin is very simple. It extends the AnalysisPlugin base class, giving the plugin the
name "V2C". It returns an instance of TranslateCommand in response to a getCommand request that
starts with "translate". Lastly, it includes a line in the help output for the command.

The LSP plugin is similarly very simple, registering itself as "V2C", but also registering to handle
UnknownMethodEvents of type "slsp/v2c". If such an event is received, the "analyse" method is called,
which performs the same processing as the TranslateCommand, except within an LSP environment, writing
the output to "output.c".

As you'll see from the classes, the processing creates a new "TR" tree of objects from
the TC (via TCPlugin) tree using the ClassMapper, and then it uses the new tree to translate
the specification into "C"... or at least very very simple specifications! Remember, this is
an example :-)

The tree-creation process uses the [tc-tr.mappings](src/main/resources/tc-tr.mappings) file. This
defines how to translate each of the (relevant) TC tree classes into TR classes. The mapping
file has more comments on the file format and the settings used.