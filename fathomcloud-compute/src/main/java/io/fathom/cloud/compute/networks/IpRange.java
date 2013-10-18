package io.fathom.cloud.compute.networks;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;

public abstract class IpRange {
    protected final InetAddress address;
    protected final int netmaskLength;
    protected final byte[] mask;
    protected final byte[] masked;

    public static IpRange build(byte[] addr, int prefixLength) {
        InetAddress address;
        try {
            address = InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Error building address", e);
        }

        return build(address, prefixLength);
    }

    private static IpRange build(InetAddress address, int prefixLength) {
        int addressLength = address.getAddress().length * 8;
        if (prefixLength == -1) {
            prefixLength = addressLength;
        }

        if (addressLength == 32) {
            return new IpV4Range(address, prefixLength);
        }

        if (addressLength == 128) {
            return new IpV6Range((Inet6Address) address, prefixLength);
        }

        throw new IllegalStateException();
    }

    public static IpRange parse(String cidr) {
        if (Strings.isNullOrEmpty(cidr)) {
            return null;
        }

        int slashPosition = cidr.indexOf('/');
        int maskLength = -1;
        String addressString;
        if (slashPosition != -1) {
            addressString = cidr.substring(0, slashPosition);

            maskLength = Integer.parseInt(cidr.substring(slashPosition + 1));
        } else {
            addressString = cidr;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve address: " + addressString, e);
        }

        return build(address, maskLength);
    }

    public IpRange(InetAddress address, int netmaskLength) {
        this.address = address;
        this.netmaskLength = netmaskLength;

        if (address instanceof Inet4Address) {
            // TODO: Cache?
            this.mask = buildMask(new byte[4], netmaskLength);
        } else if (address instanceof Inet6Address) {
            // TODO: Cache
            this.mask = buildMask(new byte[16], netmaskLength);
        } else {
            throw new IllegalArgumentException();
        }

        this.masked = address.getAddress();
        for (int i = 0; i < masked.length; i++) {
            masked[i] &= mask[i];
        }
    }

    // public abstract String getNetmask();

    public InetAddress getAddress() {
        return address;
    }

    // public Iterable<InetAddress> all() {
    // return new Iterable<InetAddress>() {
    // @Override
    // public Iterator<InetAddress> iterator() {
    // return new SimpleIterator<InetAddress>() {
    // @Override
    // protected InetAddress getNext(InetAddress current) {
    // if (current == null) {
    // return IpRange.this.getAddressInRange(0);
    // } else {
    // return IpRange.this.getNext(current);
    // }
    // }
    // };
    // }
    // };
    // }
    //
    // public byte[] getMasked() {
    // byte[] addressBytes = address.getAddress();
    // applyMask(addressBytes, netmaskLength);
    // return addressBytes;
    // }
    //
    // protected static void applyMask(byte[] addr, int length) {
    // // TODO: This is slow (ish)
    // for (int i = length; i < addr.length * 8; i++) {
    // int pos = i / 8;
    // if ((i % 8) == 0) {
    // addr[pos] = 0;
    // i += 7;
    // } else {
    // int bit = 7 - (i % 8);
    // int bitMask = 1 << bit;
    // addr[pos] &= ~bitMask;
    // }
    // }
    // }
    //

    protected byte[] getNetmaskBytes() {
        return mask;
    }

    private static byte[] buildMask(byte[] addr, int length) {
        // TOOD: This is kind of slow... implement 8 byte fast-jump?
        for (int i = 0; i < length; i++) {
            int pos = i / 8;
            int bit = 7 - (i % 8);
            int bitMask = 1 << bit;
            addr[pos] |= bitMask;
        }
        return addr;
    }

    // static void addBit(byte[] data, int addBitIndex) {
    // if (addBitIndex < 0 || addBitIndex >= (data.length * 8)) {
    // throw new IllegalArgumentException();
    // }
    //
    // int pos = addBitIndex / 8;
    // int bit = 7 - addBitIndex % 8;
    // int bitMask = 1 << bit;
    //
    // int v = data[pos] & 0xff;
    // v += bitMask;
    //
    // data[pos] = (byte) (v & 0xff);
    // if (v > 0xff) {
    // v >>= 8;
    // if (v == 1) {
    // addBit(data, addBitIndex - 8);
    // } else {
    // throw new UnsupportedOperationException();
    // }
    // }
    // }
    //
    // protected InetAddress getNext(InetAddress current) {
    // byte[] addr = current.getAddress();
    // addBit(addr, addr.length * 8 - 1);
    //
    // InetAddress next = toAddress(addr);
    // if (!isInRange(next)) {
    // return null;
    // }
    //
    // return next;
    // }
    //
    // protected InetAddress toAddress(byte[] addr) {
    // try {
    // return InetAddress.getByAddress(addr);
    // } catch (UnknownHostException e) {
    // throw new IllegalStateException(
    // "Error building address from bytes", e);
    // }
    // }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + toCidr();
    }

    public String toCidr() {
        return InetAddresses.toAddrString(address) + "/" + netmaskLength;
    }

    public int getNetmaskLength() {
        return netmaskLength;
    }

    // public InetAddress getGatewayAddress() {
    // return getAddressInRange(1);
    // }
    //
    // protected InetAddress getAddressInRange(int offset) {
    // byte[] addr = getMasked();
    // if (offset == 0) {
    // } else if (offset == 1) {
    // addBit(addr, 8 * addr.length - 1);
    // } else {
    // // TODO: Not implemented
    // throw new UnsupportedOperationException();
    // }
    //
    // return toAddress(addr);
    // }

    public boolean isIpv6() {
        return this instanceof IpV6Range;
    }

    public boolean isIpv4() {
        return this instanceof IpV4Range;
    }

    public boolean contains(InetAddress address) {
        byte[] addressBytes = address.getAddress();
        for (int i = 0; i < addressBytes.length; i++) {
            addressBytes[i] &= mask[i];
        }
        return Arrays.equals(masked, addressBytes);
    }

    @Override
    public int hashCode() {
        String cidr = toCidr();

        final int prime = 31;
        int result = 1;
        result = prime * result + ((cidr == null) ? 0 : cidr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IpRange other = (IpRange) obj;
        String cidr = toCidr();
        if (cidr == null) {
            if (other.toCidr() != null) {
                return false;
            }
        } else if (!cidr.equals(other.toCidr())) {
            return false;
        }
        return true;
    }

}
