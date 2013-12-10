#!/bin/bash

set -e
set -x

if [[ ! -f ~fathomcloud/.ssh/id_rsa ]]; then
	# Generate the ssh key
	su fathomcloud -c "ssh-keygen -q -t rsa -f ~/.ssh/id_rsa -N \"\"" -s /bin/bash
	
	# Make sure the key is authorized
	cat ~fathomcloud/.ssh/id_rsa.pub | su fathomcloud -c "tee -a ~fathomcloud/.ssh/authorized_keys"
fi
