#!/bin/bash
#####################################################################################
# Execute the DBGP client with various options
#####################################################################################

# Change these to flip version
MVERSION="4.5.0-SNAPSHOT"
PVERSION="4.5.0-P-SNAPSHOT"

# The Maven repository directory containing jars
MAVENREPO=~/.m2/repository/com/fujitsu

# Details for 64-bit Java
JAVA64="/usr/bin/java"
VM_OPTS="-Xmx3000m -Xss1m -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote"

function help()
{
    echo "Usage: $0 [--help|-?] [-P] <-vdmsl | -vdmpp | -vdmrt>"
    echo "-P use high precision VDMJ"
    echo "Default VM options are $JAVA64 $VM_OPTS"
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

# Just warn if a later version is available in Maven
LATEST=$(ls $MAVENREPO/vdmj | grep "^[0-9].[0-9].[0-9]" | tail -1)

if [ "$MVERSION" != "$LATEST" ]
then
    echo "WARNING: Latest VDMJ version is $LATEST, not $MVERSION"
fi


# Chosen version defaults to "master"
VERSION=$MVERSION
DIALECT=""

# Process non-VDMJ options
while [ $# -gt 0 ]
do
    case "$1" in
	--help|-\?)
	    help
	    ;;
	-vdmsl|-vdmpp|-vdmrt)
	    DIALECT=$1
	    ;;
	-P)
	    VERSION=$PVERSION
	    ;;
	-D*|-X*)
	    VM_OPTS="$VM_OPTS $1"
	    ;;
	*)
	    help
    esac
    shift
done

if [ "$DIALECT" = "" ]
then help
fi

# Locate the jars
VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
DBGP_JAR=$MAVENREPO/dbgp/${VERSION}/dbgp-${VERSION}.jar
DBGPC_JAR=$MAVENREPO/dbgpc/${VERSION}/dbgpc-${VERSION}.jar
check "$VDMJ_JAR"
check "$DBGP_JAR"
check "$DBGPC_JAR"
JARS="-Ddbgp.vdmj_jar=$VDMJ_JAR -Ddbgp.dbgp_jar=$DBGP_JAR"

# Keep rlwrap output in a separate folder
export RLWRAP_HOME=~/.dbgpc

# Execute the JVM...
exec rlwrap "$JAVA64" $VM_OPTS $JARS -jar $DBGPC_JAR $DIALECT

