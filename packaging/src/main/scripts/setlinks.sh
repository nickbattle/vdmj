#!/bin/bash
#####################################################################################
#  Overwrite VDM-VSCode extension jars with symlinks to Maven
######################################################################################

if [ ! -e extensions/overture* ]
then
	echo "You must run this from the root of your VSCode installation, for example $HOME/.vscode"
	exit 1
fi

if [ $# -eq 0 ]
then
	# Show existing symlinks here
	echo "Current Overture symlinks..."
	find extensions/overture* -type l
	exit 0
fi

if [ $# -ne 2 ]
then
    echo "setlinks.sh <VDM VSCode version> <VDMJ suite version>"
	echo "Do not include -SNAPSHOT in versions"
    exit 1
fi

cd $HOME/.vscode
VERSION=$1
SUITE=$2

if [ ! -e extensions/overturetool.vdm-vscode-$VERSION ]
then
    echo "Cannot find extensions/overturetool.vdm-vscode-$VERSION"
    exit 1
fi

RESOURCES="extensions/overturetool.vdm-vscode-$VERSION/resources"
GROUPID="dk/au/ece/vdmj"
SNAPSHOT="-SNAPSHOT"

for DIR in jars/vdmj jars/vdmj_hp
do
    rm $RESOURCES/$DIR/vdmj*.jar
    rm $RESOURCES/$DIR/lsp*.jar
    rm $RESOURCES/$DIR/annotations/annotations*.jar
    rm $RESOURCES/$DIR/plugins/quickcheck-*.jar
    rm $RESOURCES/$DIR/libs/stdlib*.jar
    echo "Cleaned $DIR"
done


ln -sf $HOME/.m2/repository/$GROUPID/annotations/$SUITE$SNAPSHOT/annotations-$SUITE$SNAPSHOT.jar $RESOURCES/jars/vdmj/annotations
ln -sf $HOME/.m2/repository/$GROUPID/vdmj/$SUITE$SNAPSHOT/vdmj-$SUITE$SNAPSHOT.jar $RESOURCES/jars/vdmj
ln -sf $HOME/.m2/repository/$GROUPID/lsp/$SUITE$SNAPSHOT/lsp-$SUITE$SNAPSHOT.jar $RESOURCES/jars/vdmj
ln -sf $HOME/.m2/repository/$GROUPID/stdlib/$SUITE$SNAPSHOT/stdlib-$SUITE$SNAPSHOT.jar $RESOURCES/jars/vdmj/libs
ln -sf $HOME/.m2/repository/$GROUPID/quickcheck/$SUITE$SNAPSHOT/quickcheck-$SUITE$SNAPSHOT.jar $RESOURCES/jars/vdmj/plugins
echo "Created jars/vdmj links"

ln -sf $HOME/.m2/repository/$GROUPID/annotations/$SUITE-P$SNAPSHOT/annotations-$SUITE-P$SNAPSHOT.jar $RESOURCES/jars/vdmj_hp/annotations
ln -sf $HOME/.m2/repository/$GROUPID/vdmj/$SUITE-P$SNAPSHOT/vdmj-$SUITE-P$SNAPSHOT.jar $RESOURCES/jars/vdmj_hp
ln -sf $HOME/.m2/repository/$GROUPID/lsp/$SUITE-P$SNAPSHOT/lsp-$SUITE-P$SNAPSHOT.jar $RESOURCES/jars/vdmj_hp
ln -sf $HOME/.m2/repository/$GROUPID/stdlib/$SUITE-P$SNAPSHOT/stdlib-$SUITE-P$SNAPSHOT.jar $RESOURCES/jars/vdmj_hp/libs
ln -sf $HOME/.m2/repository/$GROUPID/quickcheck/$SUITE-P$SNAPSHOT/quickcheck-$SUITE-P$SNAPSHOT.jar $RESOURCES/jars/vdmj_hp/plugins
echo "Created jars/vdmj_hp links"

echo "Done"
