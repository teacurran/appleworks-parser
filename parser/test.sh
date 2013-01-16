#!/bin/sh

mvn package
./parse.sh -f "../test_files/alice_6.2.9_osx.cwk"

./parse.sh -f "../test_files/brownfox_cfr1_os9.cwk"
