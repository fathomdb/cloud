#!/bin/bash

set -e
set -x

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

