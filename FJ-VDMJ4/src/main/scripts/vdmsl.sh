#!/bin/bash
#####################################################################################
# Execute VDMJ jar with various options
#####################################################################################

function help()
{
    echo "Usage: $0 [--help|-?] [-P] [-A] <VDMJ options>"
    echo "-P use high precision VDMJ"
    echo "-A use annotation libraries and options"
    echo "Java options are $JAVA64 $JAVA64_VMOPTS"
    echo "VDMJ installation is $JARDIR"
    echo "VDMJ options are $VDMJ_VMOPTS $VDMJ_OPTS"
    exit 0
}

function check()
{
    if [ ! -r "$0" ]
    then
	echo "Cannot read $0"
	exit 1
    fi
}

# The installation directory containing VDMJ jars
JARDIR=~/lib

# Details for 64-bit Java
JAVA64="/usr/bin/java"
JAVA64_VMOPTS="-Xmx3000m -Xss5m"

# Set defaults as standard precision without annotations
PRECISION=""
VDMJ_OPTS="-path $JARDIR/stdlib"
VERSION="4.3.0"
ANNOTATIONS_VERSION=""

if [ $# -eq 0 ]
then help
fi

# Process non-VDMJ options
while [ $# -gt 0 ]
do
    case "$1" in
	--help|-?)
	    help
	    ;;
	-A)
	    ANNOTATIONS_VERSION="1.0.0"
	    ;;
	-P)
	    PRECISION="-P"
	    ;;
	*)
	    VDMJ_OPTS="$VDMJ_OPTS $1"
    esac
    shift
done

# Locate the jars
VDMJ_JAR=$JARDIR/vdmj-${VERSION}${PRECISION}.jar
check "$VDMJ_JAR"
CLASSPATH="$VDMJ_JAR"
MAIN="com.fujitsu.vdmj.VDMJ"

if [ $ANNOTATIONS_VERSION ]
then
    ANNOTATIONS_JAR=$JARDIR/annotations-$ANNOTATIONS_VERSION.jar
    check "$ANNOTATIONS_JAR"
    ANNOTATIONS2_JAR=$JARDIR/annotations2-$ANNOTATIONS_VERSION.jar
    check "$ANNOTATIONS2_JAR"
    VDMJ_OPTS="$VDMJ_OPTS -annotations"
    VDMJ_VMOPTS="$VDMJ_VMOPTS -Dannotations.debug"
    CLASSPATH="$CLASSPATH:$ANNOTATIONS_JAR:$ANNOTATIONS2_JAR"
fi


# The dialect is based on $0, so hard-link this file as vdmsl, vdmpp and vdmrt.
DIALECT=$(basename $0)

# Keep rlwrap output in a separate folder
export RLWRAP_HOME=~/.vdmj

# Execute the JVM...
exec rlwrap "$JAVA64" $JAVA_VMOPTS $VDMJ_VMOPTS -cp $CLASSPATH $MAIN -$DIALECT $VDMJ_OPTS "$@"

