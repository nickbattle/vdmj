#
# Start an LSP server
#

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
    lsp.LSPServerVSCode -vdmsl -lsp 64828

exit 0

