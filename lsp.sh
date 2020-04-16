#
# Start an LSP server
#

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

function latest()
{
    ls -t $1 | head -1
}

java -Dlog.filename=/dev/tty \
    -cp \
$(latest "Annotations/target/annotations-1.0.0-*.jar"):\
$(latest "Annotations2/target/annotations2-1.0.0-*.jar"):\
$(latest "FJ-VDMJ4/target/vdmj-4.3.0-??????.jar"):\
$(latest "LSP/target/lsp-0.0.1-SNAPSHOT-*.jar") \
    lsp.LSPServerVSCode $1 -lsp 64828

exit 0

