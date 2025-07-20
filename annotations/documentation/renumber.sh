#!/bin/bash

if [ $# -ne 2 ]
then
    echo "Usage $0 <path to src/main/java> <path to copy location>"
    echo "eg. $0 /home/nick/eclipse.vdmj/vdmj-suite/annotations/src/main/java /tmp/copy"
    echo
    echo "Check the renumbered copies, then move them back to the original source and check-in"
    exit 1
fi

PACKAGE=$1
COPY=$2

#
# eg. renumber 1000 TypeBind
#
function renumber()
{
    echo $1 > /tmp/N

    for FILE in $(cd $PACKAGE; find annotations -name "*$2Annotation.java" -print)
    do
		N=$(cat /tmp/N)
		echo $FILE
		mkdir -p $COPY/$(dirname $FILE)
		awk -v N=$N -f renumber.awk $PACKAGE/$FILE >$COPY/$FILE 2>/tmp/N
    done
}

# annotations
renumber 6100 Printf
renumber 6200 OnFail
renumber 6300 Trace
renumber 6400 DocLink
renumber 6500 NoPOG
renumber 6600 Warning
renumber 6700 Override
renumber 6800 Conjecture
renumber 6900 DeadlineMet
renumber 7000 Separate
renumber 7100 SepRequire
renumber 7200 TypeBind
renumber 7300 TypeParam

# annotations2
renumber 8000 Assert
renumber 8100 Changes
renumber 8200 Limit
renumber 8300 Test
renumber 8400 Witness

