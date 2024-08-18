#!/bin/bash
#
# Update a VDM VSCode VSIX, using the jars in this distribution.
# Download VSIX from https://marketplace.visualstudio.com/items?itemName=overturetool.vdm-vscode
#

function usage()
{
    echo "Usage: $(basename $0) <VDM VSCode VSIX>"
    echo "e.g. $(basename $0) overturetool.vdm-vscode-1.3.7.vsix"
    exit 1
}

if [ $# -ne 1 -o "$1" = "-help" ]
then
    usage
fi

VERSION="4.6.0-SNAPSHOT"
VDMJ="vdmj"

SELF=$(which "$0")
DISTRIBUTION=$(dirname "$SELF")
VSIX=$(realpath ${1})

if [ ! -e "$VSIX" ]
then
    echo "Cannot find VDM VSCode VSIX at $VSIX"
    usage
fi

if [[ "$VSIX" != *.vsix ]]
then
    echo "File passed is not a VSIX?"
    usage
fi

echo "**************************** Updating VSIX! ****************************"
(
    cd "$DISTRIBUTION"
    rm -rf extension
    EXTRACT="extension/resources/jars"
    unzip -q "$VSIX" "$EXTRACT/$VDMJ/*"

    rm -vf $EXTRACT/$VDMJ/vdmj-*.jar
    rm -vf $EXTRACT/$VDMJ/lsp-*.jar
    rm -vf $EXTRACT/$VDMJ/annotations/annotations-*.jar
    rm -vf $EXTRACT/$VDMJ/libs/stdlib-*.jar
    rm -vf $EXTRACT/$VDMJ/plugins/quickcheck-*.jar

    cp -vf vdmj-$VERSION.jar $EXTRACT/$VDMJ
    cp -vf lsp-$VERSION.jar $EXTRACT/$VDMJ
    cp -vf annotations-$VERSION.jar $EXTRACT/$VDMJ/annotations
    cp -vf stdlib-$VERSION.jar $EXTRACT/$VDMJ/libs
    cp -vf quickcheck-$VERSION.jar $EXTRACT/$VDMJ/plugins

    zip -q "$VSIX" $(find $EXTRACT -type f)
    rm -rf extension
)
echo "**************************** FINISHED **********************************"

exit 0
