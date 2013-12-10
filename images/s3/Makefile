.PHONY: sync

all: sync
	echo "Done"

sync:
	# Wheezy-base
	mkdir -p ./fathom-cloud-images/images/wheezy-base/
	cp ../wheezy-base/image.tgz ./fathom-cloud-images/images/wheezy-base/image.tgz
	cp -r ../bootstrap/ ./fathom-cloud-images/images/
	cp ../wheezy-base/manifest.json ./fathom-cloud-images/images/wheezy-base/manifest.json
	./expand-manifest.sh ./fathom-cloud-images/images/wheezy-base/manifest.json ./fathom-cloud-images/images/wheezy-base/image.tgz
	# FathomCloud
	mkdir -p ./fathom-cloud-images/images/fathomcloud/
	cp ../fathomcloud/image.tgz ./fathom-cloud-images/images/fathomcloud/image.tgz
	cp ../fathomcloud/manifest.json ./fathom-cloud-images/images/fathomcloud/manifest.json
	./expand-manifest.sh ./fathom-cloud-images/images/fathomcloud/manifest.json ./fathom-cloud-images/images/fathomcloud/image.tgz
        # OpenStack Horizon
	mkdir -p ./fathom-cloud-images/images/openstack-horizon/
	cp ../openstack-horizon/image.tgz ./fathom-cloud-images/images/openstack-horizon/image.tgz
	cp ../openstack-horizon/manifest.json ./fathom-cloud-images/images/openstack-horizon/manifest.json
	./expand-manifest.sh ./fathom-cloud-images/images/openstack-horizon/manifest.json ./fathom-cloud-images/images/openstack-horizon/image.tgz
	# Haproxy
	mkdir -p ./fathom-cloud-images/images/haproxy/
	cp ../haproxy/image.tgz ./fathom-cloud-images/images/haproxy/image.tgz
	cp ../haproxy/manifest.json ./fathom-cloud-images/images/haproxy/manifest.json
	./expand-manifest.sh ./fathom-cloud-images/images/haproxy/manifest.json ./fathom-cloud-images/images/haproxy/image.tgz
	# Upload to S3
	s3cmd mb s3://fathom-cloud-images/
	s3cmd setacl --acl-public s3://fathom-cloud-images/
	# Multipart chunks defeat MD5 checking, so avoid them
	s3cmd --multipart-chunk-size-mb=1000 --verbose --acl-public --delete-removed sync fathom-cloud-images/* s3://fathom-cloud-images/

