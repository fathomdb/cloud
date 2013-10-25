.PHONY: wheezy-base fathomcloud openstack-horizon

all: wheezy-base fathomcloud openstack-horizon

wheezy-base:
	cd wheezy-base; make

fathomcloud: wheezy-base
	cd fathomcloud; make

openstack-horizon: wheezy-base
	cd openstack-horizon; make
