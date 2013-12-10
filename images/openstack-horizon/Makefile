.PHONY: image.tgz

all: image.tgz

image.tgz:
	rm -rf rootfs/
	mkdir -p rootfs/
	tar zxf ../wheezy-base/image.tgz -C rootfs
	# Use chroot to fix up a few things (locales, mostly)
	chroot rootfs/ apt-get update
	chroot rootfs/ apt-get upgrade
	# Copy tmp
	cp --dereference -r /home/justinsb/cloud/autonomous/auto-openstack-horizon/image rootfs/tmp/image
	chmod +x rootfs/tmp/image/install.sh
	mount -t proc none rootfs/proc
	chroot rootfs/ /tmp/image/install.sh
	umount rootfs/proc
	# Cleanup
	chroot rootfs/ apt-get clean
	rm -f rootfs/etc/ssh/ssh_host_*
	# Remove persistent device names so that eth0 comes up as eth0
	#rm image/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd rootfs/; tar cvzf ../$@ .


