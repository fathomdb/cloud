#!/bin/bash

set -e
set -x

apt-get update
apt-get upgrade -y

#================================================================================
# Install manager
#================================================================================

DEBIAN_FRONTEND=noninteractive apt-get install -y make gcc libssl-dev libpcre3-dev

mkdir ~/haproxy
cd ~/haproxy
wget http://haproxy.1wt.eu/download/1.5/src/devel/haproxy-1.5-dev19.tar.gz
tar zxf haproxy-1.5-dev19.tar.gz
cd haproxy-1.5-dev19
make TARGET=linux2628 USE_PCRE=1 USE_OPENSSL=1 USE_ZLIB=1

mkdir -p /opt/haproxy
cp haproxy /opt/haproxy/

adduser --system haproxy
addgroup haproxy
adduser haproxy haproxy

apt-get remove -y make gcc

cd ~
rm -rf ~/haproxy

#================================================================================
# Install manager
#================================================================================

mkdir -p /opt/manager
unzip /tmp/image/manager.zip -d /opt/manager

cp /tmp/image/run.sh /opt/manager/run.sh
chmod +x /opt/manager/run.sh
echo 'mgr:23:respawn:/opt/manager/run.sh' | sudo tee -a /etc/inittab


#================================================================================
# Cleanup
#================================================================================

apt-get autoremove -y
apt-get clean



