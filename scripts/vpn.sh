#!/bin/bash

set -e
set -x

CONF_DIR=~/.opensshvpn/
mkdir -p ${CONF_DIR}

VERB=$1
KEY=$2

if [[ "${KEY}" == "" ]]; then
	echo "Syntax: [up|down] <config>"
	exit 1
fi

CONF_FILE=${CONF_DIR}/${KEY}
if [[ -f ${CONF_FILE} ]]; then
	. ${CONF_FILE}
else
	echo "Configuration file not found: ${CONF_FILE}"
	exit 1
fi

function local_tunnel_exists() {
	ip link show $1 || return 1
	return 0
}

function remote_tunnel_exists() {
	ssh ${SSHUSER}@${SERVER} sudo ip link show $1 || return 1
	return 0
}

function vpn_up() {
	# Find an unused local tunnel in tun2...tun255
	# Intended as a sanity check rather than a hard limit 
	LOCAL_TUN=""
	LOCAL_TUN_ID=""
	for i in `seq 2 255`; do
		if local_tunnel_exists tun${i}; then
			continue
		fi
		LOCAL_TUN=tun${i}
		LOCAL_TUN_ID=${i}
		break
	done

	if [[ "${LOCAL_TUN}" == "" ]]; then
		echo "Unable to find an unused local tunnel"
		exit 1
	fi

	# Find an unused remote tunnel
	# TODO: this is very inefficient in terms of round-trips!
	REMOTE_TUN=""
	REMOTE_TUN_ID=""
	IP=""
	for i in `seq 2 255`; do
		if remote_tunnel_exists tun${i}; then
			continue
		fi
		REMOTE_TUN=tun${i}
		REMOTE_TUN_ID=${i}

		# We use the remote tunnel id as our address
		IP=${i}
		break
	done

	if [[ "${REMOTE_TUN}" == "" ]]; then
		echo "Unable to find an unused remote tunnel"
		exit 1
	fi

	echo "Using local tunnel ${LOCAL_TUN} and remote tunnel ${REMOTE_TUN}"

	echo "PermitTunnel yes" | ssh ${SSHUSER}@${SERVER} sudo sudo tee -a /etc/ssh/sshd_config
	ssh ${SSHUSER}@${SERVER} sudo /etc/init.d/ssh reload

	# By creating the tunnel devices now, this means that we don't need root
	# TODO: Race condition!
	ssh ${SSHUSER}@${SERVER} sudo ip tuntap add dev ${REMOTE_TUN} mode tun user ${SSHUSER} group ${SSHUSER}
	sudo ip tuntap add dev ${LOCAL_TUN} mode tun user ${USER} group ${USER}

	echo "Starting tunnel"
	ssh -S none -f -N -w${LOCAL_TUN_ID}:${REMOTE_TUN_ID} ${SSHUSER}@${SERVER}

	# TODO: Do we need to pause for the above to complete?
	#sleep 2

	# Configure local link
	sudo ip link set ${LOCAL_TUN} up
	sudo ip addr add ${PREFIX}:${IP}/${MASK} dev ${LOCAL_TUN}

	# Configure remote link
	ssh ${SSHUSER}@${SERVER} sudo ip link set ${REMOTE_TUN} up
	ssh ${SSHUSER}@${SERVER} sudo ip addr add ${PREFIX}:1/${MASK} dev ${REMOTE_TUN}

	# Dump some diagnostics
	sudo ip link show ${LOCAL_TUN}
	ssh  ${SSHUSER}@${SERVER} sudo ip link show ${REMOTE_TUN}
	ssh ${SSHUSER}@${SERVER} sudo ip tuntap show dev ${REMOTE_TUN}
	sudo ip tuntap show ${LOCAL_TUN}

	# Add a local route
	sudo ip route add 2002::/16 dev ${LOCAL_TUN}

	# Enable local forwarding / proxying
	ssh ${SSHUSER}@${SERVER} sudo ip6tables -A FORWARD -o ${REMOTE_TUN} -j ACCEPT
	ssh ${SSHUSER}@${SERVER} sudo ip6tables -A FORWARD -i ${REMOTE_TUN} -j ACCEPT
	ssh ${SSHUSER}@${SERVER} sudo ip -6 neigh add proxy ${PREFIX}:${IP} dev eth0

	mkdir -p ${CONF_DIR}/state/
	echo "LOCAL_TUN=${LOCAL_TUN}" > ${CONF_DIR}/state/${KEY}
	echo "REMOTE_TUN=${REMOTE_TUN}" >> ${CONF_DIR}/state/${KEY}

	# Ping the remote IP
	ping6 -c 3 ${PREFIX}:1 || echo "VPN failed - could not ping!"
}

function is_vpn_up() {
	if [[ -f ${CONF_DIR}/state/${KEY} ]]; then
		return 0
	else
		return 1
	fi
}

function vpn_down() {
	if ! is_vpn_up; then
		echo "VPN is not up"
		exit 1
	fi

	. ${CONF_DIR}/state/${KEY}

	if local_tunnel_exists ${LOCAL_TUN}; then
		sudo ip link del ${LOCAL_TUN}
	fi

	if remote_tunnel_exists ${REMOTE_TUN}; then
		ssh ${SSHUSER}@${SERVER} sudo ip link del ${REMOTE_TUN}
	fi

	rm ${CONF_DIR}/state/${KEY}
}

if [[ ${VERB} == "up" ]]; then
	if is_vpn_up; then
		echo "VPN already up; bringing down first"
		vpn_down
	fi
	vpn_up
elif [[ ${VERB} == "down" ]]; then
	vpn_down
elif [[ ${VERB} == "status" ]]; then
	if is_vpn_up; then
		echo "VPN is UP"
	else
		echo "VPN is DOWN"
	fi
else
	echo "Unknown verb: ${VERB}"
	exit 1
fi


