#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.5-R1.0.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/BetterDispensers
mv *.class me/exphc/BetterDispensers/
jar cf BetterDispensers.jar me/ *.yml *.java ChangeLog LICENSE
