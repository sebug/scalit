#!/bin/sh
#
# Uses the jar to extract the source code from markup.nw
# and tangle.nw and compile it, then uses the compiled tangle
# to compile directly from literate source files.
#
tangle="scala -cp scalit.jar $1 scalit.tangle.Tangle"

# If we specified another command for tangle, then we'll use this
if [ "$1" != "" ]; then
    tangle="$1"
fi

echo "* Tangling and compiling with $tangle"
mkdir -p bs/{util,markup,tangle}
$tangle util/conversions.nw > bs/util/conversions-noweb.scala
$tangle util/commandline.nw > bs/util/commandline-noweb.scala
$tangle util/filters.nw > bs/util/filters-noweb.scala
$tangle markup/markup.nw > bs/markup/markup-noweb.scala
$tangle markup/blocks.nw > bs/markup/blocks-noweb.scala
$tangle tangle/tangle.nw > bs/tangle/tangle-noweb.scala
$tangle tangle/compilesupport.nw > bs/tangle/compilesupport-noweb.scala

mkdir -p stage1
scalac -deprecation -d stage1 bs/markup/markup-noweb.scala \
    bs/util/conversions-noweb.scala bs/markup/blocks-noweb.scala \
    bs/tangle/tangle-noweb.scala bs/tangle/compilesupport-noweb.scala \
    bs/util/commandline-noweb.scala bs/util/filters-noweb.scala

if [ $? -eq 0 ]; then
    echo "* Successfully extracted the source and compiled"
    echo "  the scala tangle. Now using this build"
    
    mkdir -p bootstrap

    mkdir -p bootstrap-sources
    mkdir -p bootstrap-sources/util
    mkdir -p bootstrap-sources/markup
    mkdir -p bootstrap-sources/tangle

    stage1tangle="scala -cp stage1 scalit.tangle.Tangle"

    $stage1tangle util/conversions.nw > bootstrap-sources/util/conversions-noweb.scala
    $stage1tangle util/commandline.nw > bootstrap-sources/util/commandline-noweb.scala
    $stage1tangle util/filters.nw > bootstrap-sources/util/filters-noweb.scala
    $stage1tangle markup/markup.nw > bootstrap-sources/markup/markup-noweb.scala
    $stage1tangle markup/blocks.nw > bootstrap-sources/markup/blocks-noweb.scala
    $stage1tangle tangle/tangle.nw > bootstrap-sources/tangle/tangle-noweb.scala
    $stage1tangle tangle/compilesupport.nw > bootstrap-sources/tangle/compilesupport-noweb.scala

#    scala -cp stage1 scalit.tangle.LitComp markup/markup.nw markup/blocks.nw \
#	util/filters.nw util/commandline.nw util/conversions.nw \
#	tangle/tangle.nw tangle/compilesupport.nw \
#	-d bootstrap
    scalac bootstrap-sources/*/*.scala -d bootstrap

    if [ $? -eq 0 ]; then
	echo "* Removing old compiled files"
	rm -r stage1
	rm -r bootstrap-sources
#	rm -r bs

	echo "* Compiling with the new tangle"
	mkdir -p classes
	javac toolsupport/verbfilterScala.java -d classes

	mkdir -p stage2-sources
	mkdir -p stage2-sources/util
	mkdir -p stage2-sources/markup
	mkdir -p stage2-sources/tangle

	stage2tangle="scala -cp bootstrap scalit.tangle.Tangle"

	$stage2tangle util/conversions.nw > stage2-sources/util/conversions-noweb.scala
	$stage2tangle util/commandline.nw > stage2-sources/util/commandline-noweb.scala
	$stage2tangle util/filters.nw > stage2-sources/util/filters-noweb.scala
	$stage2tangle markup/markup.nw > stage2-sources/markup/markup-noweb.scala
	$stage2tangle markup/blocks.nw > stage2-sources/markup/blocks-noweb.scala
	$stage2tangle tangle/tangle.nw > stage2-sources/tangle/tangle-noweb.scala
	$stage2tangle tangle/compilesupport.nw > stage2-sources/tangle/compilesupport-noweb.scala

	

#	scala -cp bootstrap scalit.tangle.LitComp markup/markup.nw markup/blocks.nw \
#	    util/filters.nw util/commandline.nw util/conversions.nw \
#	    tangle/tangle.nw tangle/compilesupport.nw \
#	    -d classes
	scalac stage2-sources/*/*.scala -d classes

	if [ $? -eq 0 ]; then
	    echo "* Compilation succeeded"
	    rm -r bootstrap
	    rm -r stage2-sources
	fi
    else
	echo "!!! Could not compile with noweb-compiled tangle"
    fi
else
    echo "!!! Compilation of noweb-tangled source files failed"
fi

