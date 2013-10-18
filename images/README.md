This directory contains Makefiles and scripts to build the first "bootstrap" images.

For other images, we like packer.io, but packer requires a base image and a working cloud.  So we have to bootstrap some images.

To build the images, simple run ```sudo make```  (debootstrap requires root).

To upload to S3, ```cd s3; make```.  Note that you won't have permissions to upload to the FathomCloud buckets!
