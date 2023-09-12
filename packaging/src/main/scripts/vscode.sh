#!/bin/bash
#
# Update a VDM VSCode installation, using the jars in this distribution.
#

function usage()
{
    echo "Usage: $(basename $0) [<VDM VSCode extension location>]"
    echo "e.g. $(basename $0) \"~/.vscode/extensions/overturetool.vdm-vscode-1.3.7\""
    exit 1
}

if [ $# -gt 1 -o "$1" = "-help" ]
then
    usage
fi

SELF=$(which "$0")
DISTRIBUTION=$(dirname "$SELF")
cd "$DISTRIBUTION"

VERSION="4.5.0-P-SNAPSHOT"
RESDIR="vdmj_hp"
EXTENSION=${1:-~/.vscode/extensions/overturetool.vdm-vscode-1.3.7}

if [ ! -e "vdmj-$VERSION.jar" ]
then
    echo "Install script version $VERSION does not match distribution jars?"
    usage
fi

if [ ! -e "$EXTENSION" ]
then
    echo "Cannot find VDM VSCode extension at $EXTENSION"
    usage
fi

echo "**************************** WARNING! ****************************"
echo "This script will OVERWRITE your VDM VSCode extension with new jars"
echo "This CANNOT be undone!"
echo "If anything goes wrong, you will have to REINSTALL the extension."
echo
echo "VDM-VSCode installation = $EXTENSION"
echo "VDMJ distribution       = $DISTRIBUTION"
echo "Version                 = $VERSION"
echo

RESP=""

while [ "$RESP" != "yes" -a "$RESP" != "no" ]
do
    echo -n "Do you want to continue [yes/no]? "
    read RESP
done

if [ "$RESP" = "no" ]
then
    echo "Aborted."
    exit 0
fi

echo "**************************** COPYING! ****************************"
echo
rm -vf "$EXTENSION"/resources/jars/$RESDIR/vdmj-*.jar
rm -vf "$EXTENSION"/resources/jars/$RESDIR/lsp-*.jar
rm -vf "$EXTENSION"/resources/jars/$RESDIR/annotations-*.jar
rm -vf "$EXTENSION"/resources/jars/$RESDIR/libs/stdlib-*.jar
rm -vf "$EXTENSION"/resources/jars/$RESDIR/libs/quickcheck-*.jar 
echo
cp -vf vdmj-$VERSION.jar "$EXTENSION"/resources/jars/$RESDIR
cp -vf lsp-$VERSION.jar "$EXTENSION"/resources/jars/$RESDIR
cp -vf annotations-$VERSION.jar "$EXTENSION"/resources/jars/$RESDIR 
cp -vf stdlib-$VERSION.jar "$EXTENSION"/resources/jars/$RESDIR/libs
cp -vf quickcheck-$VERSION.jar "$EXTENSION"/resources/jars/$RESDIR/libs
echo
echo "**************************** FINISHED ****************************"
echo
echo "You must restart VSCode!"
echo "if problems, remove and re-install VDM-VSCode from the marketplace."
echo

exit 0
