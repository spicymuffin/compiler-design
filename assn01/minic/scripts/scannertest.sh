#!/bin/bash

#
# Note: this script must be run from the minic directory, otherwise the
# hardcoded paths won't work!
#
TOPDIR=`pwd`
tst=$TOPDIR/scanner/tst/base/testcases
sol=$TOPDIR/scanner/tst/base/solutions
ans=$TOPDIR/scanner/tst/base/answer
classpath=$TOPDIR/../build/classes/main
minic=$TOPDIR/../build/libs/MiniC-Scanner.jar

mkdir -p $ans
for file in $tst/c*.txt
do
     f=`basename $file`
     echo -n "."
     java -jar $minic $file > $ans/s_$f
     diff -u --ignore-all-space --ignore-blank-lines $ans/s_$f $sol/s_$f > /dev/null
     if [ "$?" -ne 0 ]
     then
		 echo
		 echo -e "\nfile $f:"
		 echo -e "test failed\n"
		 exit
     fi
done

echo -e "\nTest complete. All test cases succeeded."
