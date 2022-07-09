#!/bin/bash

function update()
{
    awk "/<!--UPDATE-->/{ sub(\"<version>.*</version>\", \"<version>\"\"$1\"\"</version>\") } 1" $2
}

if [ $# -ne 1 ]
then
    echo "Update all POM versions for VDMJ"
    echo "$0 <version>"
    exit 1
fi

VERSION="$1"
TEMP=/tmp/setVersion$$
trap "rm -f $TEMP" INTR EXIT

find . -name "pom.xml" -print | while read POM
do
    echo "Updating $POM"
    update "$VERSION" "$POM" >$TEMP
    cat $TEMP > "$POM"
done

