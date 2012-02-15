#!/bin/sh -x
CLASSPATH=../craftbukkit-1.1-R3.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/AntiDispenser
mv *.class me/exphc/AntiDispenser/
jar cf AntiDispenser.jar me/ *.yml *.java ChangeLog LICENSE
cp AntiDispenser.jar ../plugins/
