.PHONY: image.tgz

all: image.tgz

image.tgz:
	rm -rf rootfs/
	mkdir -p rootfs/
	http_proxy="http://127.0.0.1:3142" /usr/sbin/debootstrap wheezy rootfs/
	echo "nameserver 8.8.8.8" > rootfs/etc/resolv.conf
	echo "nameserver 8.8.4.4" >> rootfs/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8888" >> rootfs/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8844" >> rootfs/etc/resolv.conf
	echo "wheezy-base" > rootfs/etc/hostname
	# Install our scripts
	cp files/etc/init.d/fathomcloud-* rootfs/etc/init.d/
	chmod +x rootfs/etc/init.d/fathomcloud-*
	chroot rootfs/ insserv -d fathomcloud-get-sshkey
	# Use the install.sh script
	cp install.sh rootfs/tmp/install.sh
	chmod +x rootfs/tmp/install.sh
	http_proxy="http://127.0.0.1:3142" chroot rootfs/ /tmp/install.sh
	rm rootfs/tmp/install.sh
	# Finishing the image
	chroot rootfs/ apt-get clean
	# Remove SSH host keys
	rm rootfs/etc/ssh/ssh_host_*
	# Remove persistent device names so that eth0 comes up as eth0
	#rm image/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd rootfs/; tar cvzf ../$@ .
	echo "Created image: wheezy-base"


