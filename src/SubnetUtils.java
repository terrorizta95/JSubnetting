/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anubis.JSubnetting;

/******************************************************************************************   
 *  Autor Salvador Vera Franco based on Apache Commons Net "SubnetUtils class"            *                                                       
 *  Version 1.1                                                                           *
 *  --WildCard, NextNetworkAddress and getDefaultMask added.                              *
 ******************************************************************************************/

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SubnetUtils extends SubnetHelper{
   
    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,2})"; 
    private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
    private static final int NBITS = 32;

    private final int netmask;
    private final int address;
    private final int network;
    private final int broadcast;
    

    /** Whether the broadcast/network address are included in host count */
    private boolean inclusiveHostCount = false;


    /**
     * Constructor that takes a CIDR-notation string, e.g. "192.168.0.1/16"
     * @param cidrNotation A CIDR-notation string, e.g. "192.168.0.1/16"
     * @throws IllegalArgumentException if the parameter is invalid,
     * i.e. does not match n.n.n.n/m where n=1-3 decimal digits, m = 1-2 decimal digits in range 0-32
     */
    public SubnetUtils(String cidrNotation) {
        
      Matcher matcher = cidrPattern.matcher(cidrNotation);

      if (matcher.matches()) {
          this.address = matchAddress(matcher);
          

          /* Create a binary netmask from the number of bits specification /x */

          int trailingZeroes = NBITS - rangeCheck(Integer.parseInt(matcher.group(5)), 0, NBITS);
          
          
          /*
           * An IPv4 netmask consists of 32 bits, a contiguous sequence 
           * of the specified number of ones followed by all zeros.
           * So, it can be obtained by shifting an unsigned integer (32 bits) to the left by
           * the number of trailing zeros which is (32 - the # bits specification).
           * Note that there is no unsigned left shift operator, so we have to use
           * a long to ensure that the left-most bit is shifted out correctly.
           */
          this.netmask = (int) (0x0FFFFFFFFL << trailingZeroes );
         
          /* Calculate base network address */
          this.network = (address & netmask);
          
          /* Calculate broadcast address */
          this.broadcast = network | ~(netmask);
          System.out.println("address: "+address+"\nTrial: "+trailingZeroes+"\nnetmask: "+netmask+
                  "\nNetwork: "+network+"\nBroadcast: "+broadcast);
      } else {
          throw new IllegalArgumentException("Could not parse [" + cidrNotation + "]");
      }
    }

    /**
     * Constructor that takes a dotted decimal address and a dotted decimal mask.
     * @param address An IP address, e.g. "192.168.0.1"
     * @param mask A dotted decimal netmask e.g. "255.255.0.0"
     * @throws IllegalArgumentException if the address or mask is invalid,
     * i.e. does not match n.n.n.n where n=1-3 decimal digits and the mask is not all zeros
     */
    public SubnetUtils(String address, String mask) {
        this.address = toInteger(address);
        this.netmask = toInteger(mask);

        if ((this.netmask & -this.netmask) - 1 != ~this.netmask) {
            throw new IllegalArgumentException("Could not parse [" + mask + "]");
        }

        /* Calculate base network address */
        this.network = (this.address & this.netmask);

        /* Calculate broadcast address */
        this.broadcast = this.network | ~(this.netmask);
    }


    /**
     * Returns <code>true</code> if the return value of {@link SubnetInfo#getAddressCount()}
     * includes the network and broadcast addresses.
     * @since 2.2
     * @return true if the host count includes the network and broadcast addresses
     */
    public boolean isInclusiveHostCount() {
        return inclusiveHostCount;
    }

    /**
     * Set to <code>true</code> if you want the return value of {@link SubnetInfo#getAddressCount()}
     * to include the network and broadcast addresses.
     * @param inclusiveHostCount true if network and broadcast addresses are to be included
     * @since 2.2
     */
    public void setInclusiveHostCount(boolean inclusiveHostCount) {
        this.inclusiveHostCount = inclusiveHostCount;
    }



    /**
     * Convenience container for subnet summary information.
     *
     */
    public final class SubnetInfo {
        /* Mask to convert unsigned int to a long (i.e. keep 32 bits) */
        private static final long UNSIGNED_INT_MASK = 0x0FFFFFFFFL;

        private SubnetInfo() {}

        // long versions of the values (as unsigned int) which are more suitable for range checking
        private long networkLong()  { return network &  UNSIGNED_INT_MASK; }
        private long broadcastLong(){ return broadcast &  UNSIGNED_INT_MASK; }

        private int low() {
            return (isInclusiveHostCount() ? network :
                broadcastLong() - networkLong() > 1 ? network + 1 : 0);
        }

        private int high() {
            return (isInclusiveHostCount() ? broadcast :
                broadcastLong() - networkLong() > 1 ? broadcast -1  : 0);
        }

        /**
         * Returns true if the parameter <code>address</code> is in the
         * range of usable endpoint addresses for this subnet. This excludes the
         * network and broadcast addresses.
         * @param address A dot-delimited IPv4 address, e.g. "192.168.0.1"
         * @return True if in range, false otherwise
         */
        public boolean isInRange(String address) {
            return isInRange(toInteger(address));
        }

        /**
         *
         * @param address the address to check
         * @return true if it is in range
         * @since 3.4 (made public)
         */
        public boolean isInRange(int address) {
            long addLong = address & UNSIGNED_INT_MASK;
            long lowLong = low() & UNSIGNED_INT_MASK;
            long highLong = high() & UNSIGNED_INT_MASK;
            return addLong >= lowLong && addLong <= highLong;
        }

        /**
         *
         * @return
         */
        public String getBroadcastAddress() {
            return format(toArray(broadcast));
        }
        
        /**
         *
         * @return
         */
        public String getNextNetworkAddress() {
            return format(toArray(broadcast+1));
        }

        /**
         *
         * @return
         */
        public String getNetworkAddress() {
            return format(toArray(network));
        }
        
        /**
         *
         * @return
         */
        public String getDefaultMask(){
        return DefaultMask(getNetworkAddress());
        }

        /**
         *
         * @return
         */
        public String getNetmask() {
            return format(toArray(netmask));
        }

        /**
         *
         * @return
         */
        public String getWildcard() {
            return format(toArray((-1)-netmask));
        }
        
        /**
         *
         * @return
         */
        public String getAddress() {
            return format(toArray(address));
        }
        
        /**
         * Return the low address as a dotted IP address.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         *
         * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
         */
        public String getLowAddress() {
            return format(toArray(low()));
        }

        /**
         * Return the high address as a dotted IP address.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         *
         * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
         */
        public String getHighAddress() {
            return format(toArray(high()));
        }

        /**
         * Get the count of available addresses.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         * @return the count of addresses, may be zero.
         * @throws RuntimeException if the correct count is greater than {@code Integer.MAX_VALUE}
         * @deprecated (3.4) use {@link #getAddressCountLong()} instead
         */
        @Deprecated
        public int getAddressCount() {
            long countLong = getAddressCountLong();
            if (countLong > Integer.MAX_VALUE) {
                throw new RuntimeException("Count is larger than an integer: " + countLong);
            }
            // N.B. cannot be negative
            return (int)countLong;
        }

        /**
         * Get the count of available addresses.
         * Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
         * @return the count of addresses, may be zero.
         * @since 3.4
         */
        public long getAddressCountLong() {
            long b = broadcastLong();
            long n = networkLong();
            long count = b - n + (isInclusiveHostCount() ? 1 : -1);
            return count < 0 ? 0 : count;
        }

        /**
         *
         * @param address
         * @return
         */
        public int asInteger(String address) {
            return toInteger(address);
        }

        /**
         *
         * @return
         */
        public String getCidrSignature() {
            return format(toArray(address)) + "/" + pop(netmask);
        }

        /**
         *
         * @return
         */
        public String[] getAllAddresses() {
            int ct = getAddressCount();
            String[] addresses = new String[ct];
            if (ct == 0) {
                return addresses;
            }
            for (int add = low(), j=0; add <= high(); ++add, ++j) {
                addresses[j] = format(toArray(add));
            }
            return addresses;
        }

        /**
         * {@inheritDoc}
         * @since 2.2
         */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append("CIDR Signature:\t[").append(getCidrSignature()).append("]\n")
                .append("Netmask: [").append(getNetmask()).append("]\n")
                .append("Wildcard: [").append(getWildcard()).append("]\n")    
                .append("Network:\t[").append(getNetworkAddress()).append("]\n")
                 .append("First Address:\t[").append(getLowAddress()).append("]\n")
                 .append("Last Address:\t[").append(getHighAddress()).append("]\n")
                .append("Broadcast:\t[").append(getBroadcastAddress()).append("]\n")
                 .append("Next Network:\t[").append(getNextNetworkAddress()).append("]\n")    
                 .append("# Addresses:\t[").append(getAddressCount()).append("]\n");
            return buf.toString();
        }
    }

    /**
     * Return a {@link SubnetInfo} instance that contains subnet-specific statistics
     * @return new instance
     */
    public final SubnetInfo getInfo() { return new SubnetInfo(); }

    /*
     * Convert a dotted decimal format address to a packed integer format
     */
    private static int toInteger(String address) {
        Matcher matcher = addressPattern.matcher(address);
        if (matcher.matches()) {
            return SubnetHelper.matchAddress(matcher);
        } else {
            throw new IllegalArgumentException("Could not parse [" + address + "]");
        }
    }

    /*
     * Count the number of 1-bits in a 32-bit integer using a divide-and-conquer strategy
     * see Hacker's Delight section 5.1
     */
    int pop(int x) {
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0F0F0F0F;
        x = x + (x >>> 8);
        x = x + (x >>> 16);
        return x & 0x0000003F;
    }
 

}
