
#![Logo](/screenshots/fujitsu.png?raw=true "Logo")  VDMJ
###Description

VDMJ provides basic tool support for the VDM-SL, VDM++ and VDM-RT specification languages, written in Java. It includes a parser, a type checker, an interpreter (with arbitrary precision arithmetic), a debugger, a proof obligation generator and a combinatorial test generator with coverage recording, as well as JUnit support for automatic testing.

VDMJ is a command line tool, but it is used by the ![Overture](https://github.com/overturetool/overture) project, which adds a graphical Eclipse IDE interface (see screen shots below).

###Features

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

###Documentation
* ![User Guide](/FJ-VDMJ/documentation/UserGuide.pdf "User Guide")
* ![One Page Guide](/FJ-VDMJ/documentation/OnePageGuide.pdf "One Page Guide")
* ![High Precision Guide](/FJ-VDMJ/documentation/HighPrecisionGuide.pdf "High Precision Guide")
* ![VDMJUnit Guide](/FJ-VDMJ/documentation/VDMJUnit.pdf "VDMJUnit Guide")

###Screen Shots
![Help](/screenshots/help.jpg?raw=true "Help")
![Precondition failure](/screenshots/precondition.jpg?raw=true "Precondition failure")
![Eclipse Integration](/screenshots/eclipse.jpg?raw=true "Eclipse Integration")
![VDMJUnit](/screenshots/VDMJUnit.png?raw=true "VDMJUnit")
![High Precision](/screenshots/Precision.png?raw=true "High Precision")
