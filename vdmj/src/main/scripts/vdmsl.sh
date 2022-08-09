#!/bin/bash
#####################################################################################
# Execute VDMJ jar with various options
#####################################################################################

# Change these to flip VDMJ version
MVERSION="4.5.0-SNAPSHOT"
PVERSION="4.5.0-P-SNAPSHOT"

# The Maven repository directory containing VDMJ versions
MAVENREPO=~/.m2/repository/dk/au/ece/vdmj

# Location of the vdmj.properties file, if any. Override with -D.
PROPDIR="$HOME/lib"

# Details for 64-bit Java
JAVA64="/usr/bin/java"
VM_OPTS="-Xmx3000m -Xss1m -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote"

function help()
{
    echo "Usage: $0 [--help|-?] [-P] [-A] <VM and VDMJ options>"
    echo "-P use high precision VDMJ"
    echo "-A use annotation libraries and options"
    echo "Default VM options are $VM_OPTS"
    exit 0
}

function check()
{
    if [ ! -r "$1" ]
    then
	echo "Cannot read $1"
	exit 1
    fi
}

# Chosen version defaults to "master"
VERSION=$MVERSION

if [ $# -eq 0 ]
then help
fi

# Process non-VDMJ options
while [ $# -gt 0 ]
do
    case "$1" in
	--help|-\?)
	    help
	    ;;
	-A)
	    ANNOTATIONS_VERSION=$VERSION
	    ;;
	-P)
	    VERSION=$PVERSION
	    ;;
	-D*|-X*)
	    VM_OPTS="$VM_OPTS $1"
	    ;;
	*)
	    VDMJ_OPTS="$VDMJ_OPTS $1"
    esac
    shift
done

# Warn if a later version is available in Maven
LATEST=$(ls $MAVENREPO/vdmj | grep "^[0-9]*\.[0-9]*\.[0-9]*" | tail -1)

if [ "$VERSION" != "$LATEST" ]
then
    echo "WARNING: Latest VDMJ version is $LATEST, not $VERSION"
fi

# Locate the jars
VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
STDLIB_JAR=$MAVENREPO/stdlib/${VERSION}/stdlib-${VERSION}.jar
PLUGINS_JAR=$MAVENREPO/cmd-plugins/${VERSION}/cmd-plugins-${VERSION}.jar
check "$VDMJ_JAR"
check "$STDLIB_JAR"
check "$PLUGINS_JAR"
CLASSPATH="$VDMJ_JAR:$PLUGINS_JAR:$STDLIB_JAR:$PROPDIR"
MAIN="com.fujitsu.vdmj.VDMJ"

if [ $ANNOTATIONS_VERSION ]
then
    ANNOTATIONS_JAR=$MAVENREPO/annotations/${VERSION}/annotations-${VERSION}.jar
    check "$ANNOTATIONS_JAR"
    ANNOTATIONS2_JAR=$MAVENREPO/annotations2/${VERSION}/annotations2-${VERSION}.jar
    check "$ANNOTATIONS2_JAR"
    VDMJ_OPTS="$VDMJ_OPTS -annotations"
    VM_OPTS="$VM_OPTS -Dannotations.debug"
    CLASSPATH="$CLASSPATH:$ANNOTATIONS_JAR:$ANNOTATIONS2_JAR"
fi


# The dialect is based on $0, so hard-link this file as vdmsl, vdmpp and vdmrt.
DIALECT=$(basename $0)

if which rlwrap >/dev/null 2>&1
then
	# Keep rlwrap output in a separate folder
	export RLWRAP_HOME=~/.vdmj
	exec rlwrap "$JAVA64" $VM_OPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJ_OPTS "$@"
else
	exec "$JAVA64" $VM_OPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJ_OPTS "$@"
fi

