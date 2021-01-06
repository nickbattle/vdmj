#
# Start an LSP server
#

VERSION=4.4.1-P-SNAPSHOT
JAVA64_VMOPTS="-Xmx3000m -Xss1m -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote"

if [ $# -ne 1 ]
then
    echo "Usage: lsp.sh [-vdmsl | -vdmpp | -vdmrt]"
    exit 1
else
    case $1 in
	-vdmsl|-vdmpp|-vdmrt)
	;;

	*)
	echo "Usage: lsp.sh [-vdmsl | -vdmpp | -vdmrt]"
	exit 1
	;;
    esac
fi

# The Maven repository directory containing jars
MAVENREPO=~/.m2/repository/com/fujitsu
VDMJ_JAR=$MAVENREPO/vdmj/${VERSION}/vdmj-${VERSION}.jar
ANNOTATIONS_JAR=$MAVENREPO/annotations/${VERSION}/annotations-${VERSION}.jar
LSP_JAR=$MAVENREPO/lsp/${VERSION}/lsp-${VERSION}.jar

java ${JAVA64_VMOPTS} -Dlog.filename=/dev/stdout \
    -cp $VDMJ_JAR:$ANNOTATIONS_JAR:$LSP_JAR \
    lsp.LSPServerSocket $1 -lsp 8000 -dap 8001

exit 0

