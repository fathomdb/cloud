#!/bin/bash

set -e
set -x

# Make sure we're up to date
apt-get update

# If we don't install sudo, then anything that needs sudo just fails
# Ubuntu is very sudo reliant..
apt-get install --yes --no-install-recommends sudo

# Fix up locales
apt-get install --yes --no-install-recommends locales
echo "en_US.UTF-8 UTF-8" > /etc/locale.gen
echo "LANG=en_US.UTF-8" > /etc/default/locale
locale-gen en_US.utf8
DEBIAN_FRONTEND=noninteractive dpkg-reconfigure locales

# Install SSH
apt-get install --yes --no-install-recommends openssh-server

# For Ubuntu only (?)
rm -f rootfs/etc/init/plymouth.conf

# Lock down SSH; disable DNS
sed -i "s/#PasswordAuthentication yes/PasswordAuthentication no/g" /etc/ssh/sshd_config
/bin/echo -e "\n\nUseDNS no" >> /etc/ssh/sshd_config

# Disable most gettys
sed -i "s/.:23:respawn:.sbin.getty/#&/g" /etc/inittab

# Create mountpoints for volumes
mkdir -p /volumes/ephemeral/
mkdir -p /volumes/persistent/


# Create the SSH host keys on boot, if they haven't been created
cat > /etc/rc.local <<EOF
#!/bin/sh -e

# Create host keys if not created
hostkeys() {
        /usr/sbin/dpkg-reconfigure openssh-server
        for key in /etc/ssh/ssh_host_*.pub; do
                ssh-keygen -lf $key
        done
}
ls /etc/ssh/ssh_host_* > /dev/null || hostkeys

exit 0
EOF

chmod 755 /etc/rc.local
chown root:root /etc/rc.local

