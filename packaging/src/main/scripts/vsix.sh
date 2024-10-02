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

VERSION="4.7.0-P-SNAPSHOT"
VDMJ="vdmj_hp"

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
    
    zip -d "$VSIX" \
        $EXTRACT/$VDMJ/vdmj-\*.jar \
    	$EXTRACT/$VDMJ/lsp-\*.jar \
    	$EXTRACT/$VDMJ/annotations/annotations-\*.jar \
    	$EXTRACT/$VDMJ/libs/stdlib-\*.jar \
    	$EXTRACT/$VDMJ/plugins/quickcheck-\*.jar
    
    unzip -q "$VSIX" "$EXTRACT/$VDMJ/*"
    
    mkdir -p $EXTRACT/$VDMJ/annotations $EXTRACT/$VDMJ/libs $EXTRACT/$VDMJ/plugins

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
