
# ![Fujitsu Logo](/screenshots/fujitsu.png?raw=true "Fujitsu Logo")VDMJ
### Description

VDMJ provides basic tool support for the VDM-SL, VDM++ and VDM-RT specification languages, written in Java. It includes a parser, a type checker, an interpreter (with arbitrary precision arithmetic), a debugger, a proof obligation generator and a combinatorial test generator with coverage recording, as well as JUnit support for automatic testing and user definable annotations.

VDMJ is a command line tool, but it is used by the ![Overture](https://github.com/overturetool/overture) project, which adds a graphical Eclipse IDE interface as well as features like code generation. It is also accessible via the LSP/DAP protocols and can be used by an IDE like ![VS Code](https://code.visualstudio.com/) (see screen shots below).

The tool is designed to be easily extended or modified via user-defined analysis plugins. See the ![wiki](https://github.com/nickbattle/vdmj/wiki/VDMJ-Plugin-Architecture) for more details.

### Features

* Parses, type checks, executes and debugs VDM specifications
* Generates proof obligations
* Generates detailed code coverage for tests in LaTeX or MS doc
* Performs combinatorial tests
* Supports all three VDM dialects: VDM-SL, VDM++ and VDM-RT
* Supports plain text, LaTeX, MS doc, docx or ODF source files
* Supports international character sets in specifications (eg. Greek, Japanese or Cyrillic)
* Supports external libraries and remote control (tool integration)
* Provides JUnit support for automatic testing of specifications
* Supports arbitrary precision arithmetic
* Supports user defined annotations
* Supports user defined analysis extensions

### Documentation
* ![User Guide](/vdmj/documentation/UserGuide.pdf "User Guide")
* ![One Page Guide](/vdmj/documentation/OnePageGuide.pdf "One Page Guide")
* ![High Precision Guide](/vdmj/documentation/HighPrecisionGuide.pdf "High Precision Guide")
* ![VDMJUnit Guide](/vdmj/documentation/VDMJUnit.pdf "VDMJUnit Guide")
* ![Annotation Guide](/annotations/documentation/AnnotationGuide.pdf "Annotation Guide")
* ![Library Guide](/vdmj/documentation/LibraryGuide.pdf "Library Guide")
* ![External Format Guide](/vdmj/documentation/ExternalFormatGuide.pdf "External Format Guide")
* ![Plugin Writer's Guide](/lsp/documentation/PluginWritersGuide.pdf "Plugin Writer's Guide")

### Screen Shots
![Eclipse Integration](/screenshots/eclipse.jpg?raw=true "Eclipse Integration")
![VS Code Integration](/screenshots/vscode.png?raw=true "VS Code Integration")
![Help](/screenshots/help.jpg?raw=true "Help")
![Precondition failure](/screenshots/precondition.jpg?raw=true "Precondition failure")
![VDMJUnit](/screenshots/VDMJUnit.png?raw=true "VDMJUnit")
![High Precision](/screenshots/Precision.png?raw=true "High Precision")
