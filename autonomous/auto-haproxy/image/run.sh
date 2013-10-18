#!/bin/bash

mkdir -p /var/manager
cd /var/manager

java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -cp "/opt/manager/lib/*" io.fathom.auto.AutoHaproxyMain >> /var/manager/output.log 2>&1
