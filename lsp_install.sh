#
# Update the vscode extensions with new jars
#

function latest()
{
    ls -t $1 | head -1
}

VSEXT="$HOME/.vscode/extensions"

for dir in client_vdmsl client_vdmpp client_vdmrt
do
    cp -vf $(latest "Annotations/target/annotations-1.0.0-*.jar") $VSEXT/$dir/resources/annotations-1.0.0.jar
    cp -vf $(latest "Annotations2/target/annotations2-1.0.0-*.jar") $VSEXT/$dir/resources/annotation2-1.0.0.jar
    cp -vf $(latest "FJ-VDMJ4/target/vdmj-4.3.0-??????.jar") $VSEXT/$dir/resources/vdmj-4.3.0.jar
    cp -vf $(latest "LSP/target/lsp-0.0.1-SNAPSHOT-*.jar") $VSEXT/$dir/resources/lsp-0.0.1-SNAPSHOT.jar
done
exit 0

