.PHONY: image.tgz

all: image.tgz

image.tgz:
	rm -rf rootfs/
	mkdir -p rootfs/
	tar zxf ../wheezy-base/image.tgz -C rootfs
	# Use chroot to fix up a few things (locales, mostly)
	http_proxy="http://127.0.0.1:3142" chroot rootfs/ apt-get update
	http_proxy="http://127.0.0.1:3142" chroot rootfs/ apt-get upgrade
	# Copy tmp
	cp --dereference -r ../../autonomous/auto-fathomcloud/image rootfs/tmp/image
	chmod +x rootfs/tmp/image/install.sh
	mount -t proc none rootfs/proc
	http_proxy="http://127.0.0.1:3142" chroot rootfs/ /tmp/image/install.sh
	umount rootfs/proc
	# Cleanup
	chroot rootfs/ apt-get clean
	rm -f rootfs/etc/ssh/ssh_host_*
	rm -rf rootfs/tmp/hsperfdata_*/
	# Remove persistent device names so that eth0 comes up as eth0
	#rm image/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd rootfs/; tar cvzf ../$@ .
	echo "Created image: fathomcloud"


