#!/bin/bash
#####################################################################################
# Execute VDMJ jar with various options
#####################################################################################

# Change this to flip VDMJ version
VERSION=${VDMJ_VERSION:-4.5.0-P-SNAPSHOT}

function help()
{
    echo "Usage: $0 [-help] <VM and VDMJ options>"
    exit 0
}

if [ $# -eq 0 ]
then help
fi

VMOPTS=-Xmx2g
VDMJOPTS=

# Process non-VDMJ options
while [ $# -gt 0 ]
do
    case "$1" in
	--help|-\?)
	    help
	    ;;
	-D*|-X*)
	    VMOPTS="$VMOPTS $1"
	    ;;
	*)
		VDMJOPTS="$VDMJOPTS $1"
		;;
    esac
    shift
done

EXE=$(which "$0")
DIR=$(dirname "$EXE")

# Locate the jars
VDMJ_JAR="$DIR/vdmj-${VERSION}.jar"
STDLIB_JAR="$DIR/stdlib-${VERSION}.jar"
PLUGINS_JAR="$DIR/cmd-plugins-${VERSION}.jar"
ANNOTATIONS_JAR="$DIR/annotations-${VERSION}.jar"
ANNOTATIONS2_JAR="$DIR/annotations2-${VERSION}.jar"
CLASSPATH="$VDMJ_JAR:$ANNOTATIONS_JAR:$ANNOTATIONS2_JAR:$PLUGINS_JAR:$STDLIB_JAR"
MAIN="VDMJ"

if which rlwrap >/dev/null 2>&1
then
	# Keep rlwrap output in a separate folder
	export RLWRAP_HOME=~/.vdmj
	exec rlwrap java $VMOPTS -cp $CLASSPATH $MAIN $VDMJOPTS
else
	exec java $VMOPTS -cp $CLASSPATH $MAIN $VDMJOPTS
fi
