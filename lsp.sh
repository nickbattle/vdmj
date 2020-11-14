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

java -Xmx2g -Dlog.filename=/dev/tty \
    -cp \
$(latest "Annotations/target/annotations-P-4.4.0-*.jar"):\
$(latest "FJ-VDMJ4/target/vdmj-4.4.0-P-SNAPSHOT-??????.jar"):\
$(latest "LSP/target/lsp-4.4.0-P-*.jar") \
    lsp.LSPServerSocket $1 -lsp 8000 -dap 8001

exit 0

