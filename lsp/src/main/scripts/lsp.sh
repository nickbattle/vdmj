#
# Start an LSP server
#

function usage()
{
    echo "Usage: lsp.sh [-P] [-X<arg>] [-D<name>=<value>] <-vdmsl | -vdmpp | -vdmrt>"
    echo "-P = high precision build"
    echo "-X = set JVM -X options"
    echo "-D = set Java properties"
    exit 1
}

MVERSION="4.5.0-SNAPSHOT"
PVERSION="4.5.0-P-SNAPSHOT"
VERSION=$MVERSION

# The Maven repository directory containing jars
MAVENREPO=~/.m2/repository/dk/au/ece/vdmj
ISABELLEREPO=~/.m2/repository/vdmtoolkit

JAVA64_VMOPTS="-Xmx3000m -Xss1m"
#JAVA64_VMOPTS="-Xmx3000m -Xss1m -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote"

while [ $# -gt 0 ]
do
    case $1 in
	-vdmsl|-vdmpp|-vdmrt)
	DIALECT=$1
	shift
	;;

	-P)
	VERSION=$PVERSION
	shift
	;;

	-X*|-D*)
	JAVA64_VMOPTS="$JAVA64_VMOPTS $1"
	shift
	;;

	*)
	usage
	;;
    esac
done

if [ "$DIALECT" = "" ]
then
    usage
fi

VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
ANNOTATIONS_JAR=$MAVENREPO/annotations/${VERSION}/annotations-${VERSION}.jar
LSP_JAR=$MAVENREPO/lsp/${VERSION}/lsp-${VERSION}.jar
STDLIB_JAR=$MAVENREPO/stdlib/${VERSION}/stdlib-${VERSION}.jar
QUICKCHECK_JAR=$MAVENREPO/quickcheck/${VERSION}/quickcheck-${VERSION}.jar
# ISABELLE_JARS=$ISABELLEREPO/vdm2isa/1.2.0-SNAPSHOT/vdm2isa-1.2.0-SNAPSHOT.jar:$ISABELLEREPO/vdm2isa-lsp/1.2.0-SNAPSHOT/vdm2isa-lsp-1.2.0-SNAPSHOT.jar

exec java ${JAVA64_VMOPTS} \
    -cp $VDMJ_JAR:$ANNOTATIONS_JAR:$LSP_JAR:$STDLIB_JAR:$QUICKCHECK_JAR \
    lsp.LSPServerDebug $DIALECT -lsp 8000 -dap 8001

