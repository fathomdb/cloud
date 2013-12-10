.PHONY: wheezy-base fathomcloud

all: wheezy-base fathomcloud openstack-horizon haproxy

wheezy-base:
	cd wheezy-base; make

fathomcloud: wheezy-base
	cd fathomcloud; make

openstack-horizon: wheezy-base
	cd openstack-horizon; make

haproxy: wheezy-base
	cd haproxy; make
