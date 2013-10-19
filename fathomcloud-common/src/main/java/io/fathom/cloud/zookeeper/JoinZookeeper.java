package io.fathom.cloud.zookeeper;

public class JoinZookeeper {
    /*
     * Removed until Zookeeper 3.5...
     * 
     * public void join(String id, String connectString, String me) throws
     * KeeperException, InterruptedException, IOException { ZookeeperClient
     * client = new ZookeeperClient(connectString); ZooKeeper zk =
     * client.getZk();
     * 
     * Stat stat = new Stat();
     * 
     * byte[] config = zk.getConfig(false, stat); String configStr = new
     * String(config, Charsets.UTF_8);
     * 
     * // TODO: Check id not in use ?
     * 
     * // TODO: Auto-assign ID by connecting to ZK and registering ?
     * 
     * // ZookeeperStateStore zookeeperStateStore = new //
     * ZookeeperStateStore(client);
     * 
     * String conf = "server." + id + "=" + me; // + ":2182:2183:observer;" + //
     * me + ":2181";
     * 
     * List<String> joiningServers = Lists.newArrayList(conf); List<String>
     * leavingServers = null; List<String> newMembers = null; long fromConfig =
     * stat.getVersion();
     * 
     * stat = new Stat(); config = zk.reconfig(joiningServers, leavingServers,
     * newMembers, fromConfig, stat); configStr = new String(config,
     * Charsets.UTF_8); }
     */
}
