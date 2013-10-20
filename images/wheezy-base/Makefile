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
	# Use chroot to fix up a few things (locales, mostly)
	chroot rootfs/ apt-get update
	chroot rootfs/ apt-get install --yes --no-install-recommends locales
	# If we don't install sudo, then anything that needs sudo just fails
	# Ubuntu is very sudo reliant..
	chroot rootfs/ apt-get install --yes --no-install-recommends sudo
	# Fix up locales
	echo "en_US.UTF-8 UTF-8" > rootfs/etc/locale.gen
	echo "LANG=en_US.UTF-8" > rootfs/etc/default/locale
	chroot rootfs/ locale-gen en_US.utf8
	chroot rootfs/ /bin/bash -c "DEBIAN_FRONTEND=noninteractive dpkg-reconfigure locales"
	# Install SSH
	chroot rootfs/ apt-get install --yes --no-install-recommends openssh-server
	# For Ubuntu only (?)
	rm -f rootfs/etc/init/plymouth.conf
	# Lock down SSH; disable DNS
	sed -i "s/#PasswordAuthentication yes/PasswordAuthentication no/g" rootfs/etc/ssh/sshd_config
	/bin/echo -e "\n\nUseDNS no" >>  rootfs/etc/ssh/sshd_config
	# Disable most gettys
	sed -i "s/.:23:respawn:.sbin.getty/#&/g" rootfs/etc/inittab
	# Install our scripts
	cp files/etc/init.d/fathomcloud-* rootfs/etc/init.d/
	chmod +x rootfs/etc/init.d/fathomcloud-*
	chroot rootfs/ insserv -d fathomcloud-get-sshkey
	# Use an install.sh script
	cp install.sh rootfs/tmp/install.sh
	chmod +x rootfs/tmp/install.sh
	chroot rootfs/ /tmp/install.sh
	# Finishing the image
	chroot rootfs/ apt-get upgrade
	chroot rootfs/ apt-get clean
	# Remove SSH host keys
	rm rootfs/etc/ssh/ssh_host_*
	# Remove persistent device names so that eth0 comes up as eth0
	#rm image/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd rootfs/; tar cvzf ../$@ .
	echo "Created image: wheezy-base"


