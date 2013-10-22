package io.fathom.cloud.compute.networks;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * An AddressPool is a helper class, that stores a list of ip ranges (or ips).
 * 
 * It is used by {@link NetworkPoolBase} and subclasses.
 */
class AddressPool {
    private static final Logger log = LoggerFactory.getLogger(AddressPool.class);

    final List<Block> blocks = Lists.newArrayList();

    protected InetAddress convertSeedToIp(long seed) {
        if (this.blocks.isEmpty()) {
            throw new IllegalStateException("No IP blocks defined in address pools");
        }

        seed = Math.abs(seed);

        long total = 0;
        for (Block block : this.blocks) {
            long n = block.size();
            total += n;
        }

        seed %= total;
        long cumulative = 0;
        for (Block block : this.blocks) {
            long n = block.size();
            if ((cumulative + n) > seed) {
                return block.get(seed - cumulative);
            }
            cumulative += n;
        }

        throw new IllegalStateException();
    }

    static class Block {
        final IpRange range;
        final List<InetAddress> exclusions;

        public Block(IpRange range, List<InetAddress> exclusions) {
            this.range = range;
            this.exclusions = Lists.newArrayList();
            for (InetAddress exclusion : exclusions) {
                if (range.contains(exclusion)) {
                    this.exclusions.add(exclusion);
                }
            }
        }

        public InetAddress get(long seed) {
            byte[] mask = range.getNetmaskBytes();
            byte[] prefix = range.getAddress().getAddress();

            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(seed);
            byte[] seedBytes = buffer.array();

            byte[] ip = new byte[mask.length];
            System.arraycopy(seedBytes, seedBytes.length - ip.length, ip, 0, ip.length);

            // Apply netmask
            for (int i = 0; i < mask.length; i++) {
                ip[i] &= ~mask[i];
                ip[i] |= (prefix[i] & mask[i]);
            }

            // if (ip.length == 16) {
            // // We always allocate a /112 for IPv6
            // ip[14] = 0;
            // ip[15] = 0;
            // }

            InetAddress address = NetworkPools.toAddress(ip);
            if (exclusions.contains(address)) {
                log.info("Won't choose excluded address: {}", address);
                return null;
            }

            // Addresses that end in .0 are confusing
            if (ip[ip.length - 1] == 0) {
                log.info("Won't choose address ending in 0: {}", address);
                return null;
            }

            log.debug("Mapped {} to {}", seed, address);

            return address;
        }

        public long size() {
            int bits = range.isIpv4() ? 32 : 128;
            bits -= range.getNetmaskLength();

            if (bits >= 63) {
                return Long.MAX_VALUE;
            }

            long total = 1L << bits;
            return total;
        }
    }

    public void add(IpRange ipRange, List<InetAddress> exclusions) {
        this.blocks.add(new Block(ipRange, exclusions));
    }
}
