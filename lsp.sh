#
# Start an LSP server
#

java \
    -Dlog.filename=/dev/tty \
    -cp FJ-VDMJ4/target/vdmj-4.3.0-200411.jar:LSP/target/lsp-0.0.1-SNAPSHOT-200411.jar \
    lsp.LSPServerVSCode -vdmsl -lsp 64828
