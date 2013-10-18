package io.fathom.cloud.storage.api.os.models;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This logic is meant to mirror this:
 * https://github.com/openstack/swift/blob/master/swift/common/middleware/acl.py
 * 
 */
public class StorageAcl {
    private static final Set<String> REF_TAGS = Sets.newHashSet(".r", ".ref", ".referer", ".referrer");

    private final List<UserAcl> users;
    private final List<ReferrerAcl> referers;

    static class UserAcl {
        final String group;
        final String user;

        public UserAcl(String group, String user) {
            this.group = group;
            this.user = user;
        }

        @Override
        public String toString() {
            if (!Strings.isNullOrEmpty(group)) {
                return group + ":" + user;
            } else {
                return user;
            }
        }
    }

    static class ReferrerAcl {
        final boolean deny;
        final String referer;

        public ReferrerAcl(boolean deny, String referer) {
            this.deny = deny;
            this.referer = referer;
        }

        @Override
        public String toString() {
            return ".r:" + (deny ? "-" : "") + referer;
        }
    }

    public StorageAcl(List<UserAcl> users, List<ReferrerAcl> referers) {
        this.users = users;
        this.referers = referers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Joiner.on(',').join(users));
        if (sb.length() != 0) {
            sb.append(",");
        }
        sb.append(Joiner.on(',').join(referers));
        return sb.toString();
    }

    public enum AclType {
        Read, Write
    }

    public static StorageAcl parse(AclType aclType, String s) {
        List<UserAcl> users = Lists.newArrayList();
        List<ReferrerAcl> referers = Lists.newArrayList();

        for (String token : Splitter.on(',').split(s)) {
            token = token.trim();
            if (!token.isEmpty()) {
                int colonIndex = token.indexOf(':');
                if (colonIndex == -1) {
                    users.add(new UserAcl(null, token));
                } else {
                    String first = token.substring(0, colonIndex).trim();
                    String second = token.substring(colonIndex + 1).trim();

                    if (Strings.isNullOrEmpty(first) || first.charAt(0) != '.') {
                        users.add(new UserAcl(first, second));
                    } else if (REF_TAGS.contains(first)) {
                        if (aclType != AclType.Read) {
                            throw new IllegalArgumentException("Refererrers not allowed in write ACL: " + token);
                        }

                        boolean deny = false;
                        if (second.startsWith("-")) {
                            deny = true;
                            second = second.substring(1).trim();
                        }

                        if (second.startsWith("*") && !second.equals("*")) {
                            second = second.substring(1).trim();
                        }

                        if (second.isEmpty() || second.equals(".")) {
                            throw new IllegalArgumentException("No host/domain value in ACL: " + token);
                        }

                        referers.add(new ReferrerAcl(deny, second));
                    } else {
                        throw new IllegalArgumentException("Unknown designator in ACL: " + token);
                    }
                }
            }
        }

        return new StorageAcl(users, referers);
    }

    public boolean isRefererAllowed(String referer) {
        if (!referers.isEmpty()) {
            String hostname = null;

            if (referer != null) {
                referer = referer.trim();

                URI uri = URI.create(referer);
                hostname = uri.getHost();
            }

            if (hostname != null) {
                hostname = hostname.toLowerCase();
            } else {
                hostname = "unknown";
            }

            for (ReferrerAcl acl : referers) {
                boolean isMatch = false;
                String find = acl.referer;
                if (find.equals("*") || find.equals(hostname)) {
                    isMatch = true;
                } else if (find.startsWith(".") && hostname.endsWith(find)) {
                    isMatch = true;
                }

                if (isMatch) {
                    if (acl.deny) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
