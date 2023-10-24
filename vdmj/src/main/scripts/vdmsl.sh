#!/bin/bash
#####################################################################################
# Execute VDMJ jar with various options
#####################################################################################

# Change these to flip VDMJ version
MVERSION=${VDMJ_VERSION:-4.5.0-SNAPSHOT}
PVERSION=${VDMJ_PVERSION:-4.5.0-P-SNAPSHOT}

# The Maven repository directory containing VDMJ versions
MAVENREPO=~/.m2/repository/dk/au/ece/vdmj

# Details for 64-bit Java
JAVA64="/usr/bin/java"
VMOPTS=${VDMJ_VMOPTS:--Xmx3000m -Xss1m -Dannotations.debug -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote}
VDMJOPTS=${VDMJ_OPTS:--strict -annotations}

function help()
{
    echo "Usage: $0 [--help|-?] [-P] <VM and VDMJ options>"
    echo "-P use high precision VDMJ ($PVERSION)"
    echo "Set \$VDMJ_VMOPTS and/or \$VDMJ_OPTS to override Java/tool options"
    echo "Set \$VDMJ_VERSION and \$VDMJ_PVERSION to change versions"
    echo "Set \$VDMJ_ANNOTATIONS and/or \$VDMJ_CLASSPATH for extensions" 
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

function latest()
{
	# Warn if a later version is available in Maven
	BASEVER=$(echo $1 | sed -e "s/\(^[0-9]*\.[0-9]*\.[0-9]*\(-P\)\{0,1\}\).*$/\1/")
	
	if [ -e $MAVENREPO/vdmj/$BASEVER ]
	then
		LATEST=$BASEVER
	elif [[ $1 == *-P* ]]
	then
		LATEST=$(ls $MAVENREPO/vdmj | grep "^[0-9]*\.[0-9]*\.[0-9]*" | grep -- "-P" | tail -1)
	else
		LATEST=$(ls $MAVENREPO/vdmj | grep "^[0-9]*\.[0-9]*\.[0-9]*" | grep -v -- "-P" | tail -1)
	fi
	
	if [ "$1" != "$LATEST" ]
	then
	    echo "WARNING: Latest VDMJ version is $LATEST, not $1"
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
	-P)
	    VERSION=$PVERSION
	    ;;
	-D*|-X*)
	    VMOPTS="$VMOPTS $1"
	    ;;
	*)
	    VDMJOPTS="$VDMJOPTS $1"
    esac
    shift
done

# Warn if a later version is available in Maven
latest $VERSION

# Locate the jars
VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
STDLIB_JAR=$MAVENREPO/stdlib/${VERSION}/stdlib-${VERSION}.jar
COMMANDS_JAR=$MAVENREPO/cmd-plugins/${VERSION}/cmd-plugins-${VERSION}.jar
QUICKCHECK_JAR=$MAVENREPO/quickcheck/${VERSION}/quickcheck-${VERSION}.jar
ANNOTATIONS_JAR=$MAVENREPO/annotations/${VERSION}/annotations-${VERSION}.jar
ANNOTATIONS2_JAR=$MAVENREPO/annotations2/${VERSION}/annotations2-${VERSION}.jar

check "$VDMJ_JAR"
check "$STDLIB_JAR"
check "$COMMANDS_JAR"
check "$QUICKCHECK_JAR"
check "$ANNOTATIONS_JAR"
check "$ANNOTATIONS2_JAR"

CLASSPATH="$VDMJ_JAR:$COMMANDS_JAR:$STDLIB_JAR:$QUICKCHECK_JAR:$ANNOTATIONS_JAR:$ANNOTATIONS2_JAR:$VDMJ_ANNOTATIONS:$VDMJ_CLASSPATH"
MAIN="VDMJ"

# The dialect is based on $0, so hard-link this file as vdmsl, vdmpp and vdmrt.
DIALECT=$(basename $0)

if [ "$VDMJ_DEBUG" ]
then
	echo "$JAVA64 $VMOPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJOPTS $@"
fi

if which rlwrap >/dev/null 2>&1
then
	# Keep rlwrap output in a separate folder
	export RLWRAP_HOME=~/.vdmj
	exec rlwrap "$JAVA64" $VMOPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJOPTS "$@"
else
	exec "$JAVA64" $VMOPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJOPTS "$@"
fi

