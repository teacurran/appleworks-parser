#!/bin/sh

mvn package
./parse.sh -f "../test_files/alice_6.2.9_osx.cwk" -o

./parse.sh -f "../test_files/brownfox_cfr1_os9.cwk" -o

./parse.sh -f "../test_files/alice_landscape_6.2.9_osx.cwk" -o
