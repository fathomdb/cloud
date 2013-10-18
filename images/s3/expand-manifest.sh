#!/bin/bash

set -e
set -x

MANIFEST=$1
IMAGE=$2

CHECKSUM=`md5sum ${IMAGE} | cut -f1 -d' '`
sed -i "s/{{CHECKSUM}}/${CHECKSUM}/g" ${MANIFEST}
	

