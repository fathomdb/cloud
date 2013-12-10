#!/bin/bash

set -e
set -x

# Fix hostname
echo "fathomcloud" > /etc/hostname
echo -e "127.0.0.1\tfathomcloud" >> /etc/hosts

# Set up volume paths
mkdir -p /volumes/ephemeral
mkdir -p /volumes/persistent

apt-get update
apt-get upgrade -y

DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y unzip mime-support

DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y openjdk-7-jre-headless

adduser --system fathomcloud
# We can't have false for sftp :-(
chsh -s /bin/bash fathomcloud


echo "deb http://apt-fathomdb.s3.amazonaws.com wheezy main" | sudo tee /etc/apt/sources.list.d/fathomdb.list
wget -O - https://apt-fathomdb.s3.amazonaws.com/packaging@fathomdb.com.gpg.key | sudo apt-key add -
apt-get update
apt-get install --yes applyd

# Don't allow any firewall rules present now to be copied over...
rm /etc/apply.d/*/00-saved

cd ~
wget http://apache.osuosl.org/zookeeper/zookeeper-3.4.5/zookeeper-3.4.5.tar.gz
tar zxf zookeeper-3.4.5.tar.gz
rm zookeeper-3.4.5.tar.gz
sudo mv zookeeper-3.4.5 /opt/zookeeper

sudo adduser --group --system zk

mkdir -p /opt/fathomcloud
unzip /tmp/image/server.zip -d /opt/fathomcloud

mkdir -p /opt/manager
unzip /tmp/image/manager.zip -d /opt/manager

cp /tmp/image/run.sh /opt/manager/run.sh
chmod +x /opt/manager/run.sh
echo 'c10d:23:respawn:/opt/manager/run.sh >> /var/log/manager-run.log' | sudo tee -a /etc/inittab

cp /tmp/image/keygen.sh /opt/manager/keygen.sh
chmod +x /opt/manager/keygen.sh


# Cleanup
sudo apt-get autoremove -y
rm -rf /tmp/image/
