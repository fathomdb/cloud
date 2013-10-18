#!/bin/bash

set -x
set -e

PRG="$0"
while [ -h "$PRG" ] ; do
   PRG=`readlink "$PRG"`
done

pushd `dirname $PRG` > /dev/null
BASEDIR=`pwd`
popd > /dev/null


pushd ${BASEDIR}/fathomcloud-common/src/main/protobuf
protoc *.proto --java_out .
popd

for PROJECT in fathomcloud-compute fathomcloud-image fathomcloud-identity fathomcloud-storage fathomcloud-networking fathomcloud-dbaas fathomcloud-dns fathomcloud-secrets fathomcloud-lbaas
do
pushd  ${BASEDIR}/${PROJECT}/src/main/protobuf
protoc --proto_path=${BASEDIR}/${PROJECT}/src/main/protobuf/ --proto_path=${BASEDIR}/fathomcloud-common/src/main/protobuf/ --java_out .  ${BASEDIR}/${PROJECT}/src/main/protobuf/*.proto
popd
done
