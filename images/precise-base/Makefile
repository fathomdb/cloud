.PHONY:

all: wheezy-base.tgz

wheezy-base.tgz:
	rm -rf roots/$@/
	mkdir -p roots/$@/
	http_proxy="http://127.0.0.1:3142" /usr/sbin/debootstrap wheezy roots/$@
	echo "nameserver 8.8.8.8" > roots/$@/etc/resolv.conf
	echo "nameserver 8.8.4.4" >> roots/$@/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8888" >> roots/$@/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8844" >> roots/$@/etc/resolv.conf
	# Use chroot to fix up a few things (locales, mostly)
	chroot roots/$@/ apt-get update
	chroot roots/$@/ apt-get install --yes --no-install-recommends locales
	# If we don't install sudo, then anything that needs sudo just fails
	# Ubuntu is very sudo reliant..
	chroot roots/$@/ apt-get install --yes --no-install-recommends sudo
	# Fix up locales
	echo "en_US.UTF-8 UTF-8" > chroot roots/$@/etc/locale.gen
	echo "LANG=en_US.UTF-8" > chroot roots/$@/etc/default/locale
	chroot roots/$@/ locale-gen en_US.utf8
	chroot roots/$@/ /bin/bash -c "DEBIAN_FRONTEND=noninteractive dpkg-reconfigure locales"
	# Install SSH
	chroot roots/$@/ apt-get install --yes --no-install-recommends openssh-server
	# For Ubuntu only (?)
	rm -f roots/$@/etc/init/plymouth.conf
	# Lock down SSH; disable DNS
	sed -i "s/#PasswordAuthentication yes/PasswordAuthentication no/g" roots/$@/etc/ssh/sshd_config
	/bin/echo -e "\n\nUseDNS no" >>  roots/$@/etc/ssh/sshd_config
	# Disable most gettys
	sed -i "s/.:23:respawn:.sbin.getty/#&/g" roots/$@/etc/inittab
	# Finishing the image
	chroot roots/$@/ apt-get upgrade
	chroot roots/$@/ apt-get clean
	# Remove persistent device names so that eth0 comes up as eth0
	#rm image/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd roots/$@/; tar cvzf ../../$@ .
	echo "Upload with: glance image-create --name 'wheezy6' --disk-format raw --container-format tar --file wheezy-base.tgz --is-public True"


precise-base.tgz:
	rm -rf roots/$@/
	mkdir -p roots/$@/
	http_proxy="http://127.0.0.1:3142" /usr/sbin/debootstrap precise roots/$@
	echo "nameserver 8.8.8.8" > roots/$@/etc/resolv.conf
	echo "nameserver 8.8.4.4" >> roots/$@/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8888" >> roots/$@/etc/resolv.conf
	echo "nameserver 2001:4860:4860::8844" >> roots/$@/etc/resolv.conf
	# Use chroot to fix up a few things (locales, mostly)
	chroot roots/$@/ apt-get update
	chroot roots/$@/ apt-get install --yes --no-install-recommends locales
	# If we don't install sudo, then anything that needs sudo just fails
	# Ubuntu is very sudo reliant..
	chroot roots/$@/ apt-get install --yes --no-install-recommends sudo
	# Fix up locales
	echo "en_US.UTF-8 UTF-8" > chroot roots/$@/etc/locale.gen
	echo "LANG=en_US.UTF-8" > chroot roots/$@/etc/default/locale
	chroot roots/$@/ locale-gen en_US.utf8
	chroot roots/$@/ /bin/bash -c "DEBIAN_FRONTEND=noninteractive dpkg-reconfigure locales"
	# Install SSH
	chroot roots/$@/ apt-get install --yes --no-install-recommends openssh-server
	# For Ubuntu only (?)
	rm -f roots/$@/etc/init/plymouth.conf
	# Lock down SSH; disable DNS
	sed -i "s/#PasswordAuthentication yes/PasswordAuthentication no/g" roots/$@/etc/ssh/sshd_config
	/bin/echo -e "\n\nUseDNS no" >>  roots/$@/etc/ssh/sshd_config
	# Disable most gettys
	# Grrrr... ubuntu uses upstart, not inittab
	#sed -i "s/.:23:respawn:.sbin.getty/#&/g" roots/$@/etc/inittab
	# Finishing the image
	chroot roots/$@/ apt-get upgrade
	chroot roots/$@/ apt-get clean
	# Remove persistent device names so that eth0 comes up as eth0
	rm roots/$@/etc/udev/rules.d/70-persistent-net.rules
	# Package the image
	cd roots/$@/; tar cvzf ../../$@ .
	echo "Upload with: glance image-create --name 'precise' --disk-format raw --container-format tar --file precise-base.tgz --is-public True"

etcd.tgz: wheezy-base.tgz
	mkdir -p github
	# Checkout and build etcd
	cd github; rm -rf etcd; git clone https://github.com/coreos/etcd.git
	cd github/etcd; ./build
	# Create image from base
	rm -rf roots/$@/
	mkdir -p roots/$@/
	cd roots/$@; tar zxf ../../wheezy-base.tgz
	# Copy etcd
	cp github/etcd/etcd roots/$@/usr/sbin/etcd
	# Add etcd to inittab
	echo -e "\n" >> roots/$@/etc/inittab
	echo -e "etcd:2345:respawn:/usr/sbin/etcd\n" >> roots/$@/etc/inittab
	# Package the image
	cd roots/$@/; tar cvzf ../../$@ .

