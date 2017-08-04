VDMJ Scripts

The scripts in this folder may be useful in a command line environment, either on Linux or
Cygwin on Windows.

1. vdmsl

This script will launch the most recent VDMJ jar, allowing you to select the high-precision
build or 32/64-bit JVMs. The script should be copied three times (ideally hard-linked, unchanged)
and renamed as "vdmsl", "vdmpp" and "vdmrt" in order to launch the given dialect. Change the
variables at the top of the script to match your own installation. The current values can be
listed with --help. Note that the script depends on rlwrap(1).

For example:

$ vdmsl --help
Usage: vdmsl [--help] [-P] [-32|-64] <VDMJ options>
Java 32-bit is /cygdrive/C/Program Files (x86)/Java/jre8/bin/javaw.exe -Xmx1000m -Xss5m
Java 64-bit is /cygdrive/c/Program Files/Java/jre7/bin/javaw.exe -Xmx3000m -Xss5m
VDMJ installation is C:/Cygwin/home/lib
VDMJ default arguments are -path C:/Cygwin/home/lib/stdlib
$

$ vdmsl -i MATH.vdm
Parsed 1 module in 0.185 secs. No syntax errors
Type checked 1 module in 0.178 secs. No type errors
Initialized 1 module in 0.165 secs.
Interpreter started
> p 123**123
= 1.1437436793461719E257
Executed in 0.012 secs.
> q
Bye

$ vdmsl -P -i MATH.vdm
Parsed 1 module in 0.114 secs. No syntax errors
Type checked 1 module in 0.176 secs. No type errors
Initialized 1 module in 0.341 secs.
Interpreter started
> p 123**123
= 114374367934617190099880295228066276746218078451850229775887975052369504785666896446606568365201542169649974727730628842345343196581134895919942820874449837212099476648958359023796078549041949007807220625356526926729664064846685758382803707100766740220839267
Executed in 0.005 secs.
> q
Bye
