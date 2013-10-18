package io.fathom.cloud.state.memory;

//
//import java.util.List;
//import java.util.Map;
//
//import org.apache.zookeeper.txn.Txn;
//
//import io.fathom.cloud.state.ConcurrentUpdateException;
//import io.fathom.cloud.state.IdProvider;
//import io.fathom.cloud.state.StateStore;
//import io.fathom.cloud.state.StateStoreException;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.google.protobuf.ByteString;
//
//public class InMemoryStateStore extends StateStore {
//
//	public static class InMemoryStateNode extends StateNode {
//		final InMemoryStateNode parent;
//		final String key;
//		final Map<String, InMemoryStateNode> children = Maps.newHashMap();
//
//		long version;
//
//		ByteString data;
//
//		long childChangeCount;
//
//		public InMemoryStateNode(InMemoryStateNode parent, String key) {
//			this.parent = parent;
//			this.key = key;
//			this.version = 0;
//		}
//
//		@Override
//		public List<String> getChildrenKeys() {
//			synchronized (this) {
//				return Lists.newArrayList(children.keySet());
//			}
//		}
//
//		@Override
//		public StateNode child(String key) {
//			synchronized (this) {
//				InMemoryStateNode node = children.get(key);
//				if (node == null) {
//					node = new InMemoryStateNode(this, key);
//					children.put(key, node);
//				}
//				return node;
//			}
//		}
//
//		@Override
//		public ByteString read() {
//			synchronized (this) {
//				if (version != null) {
//					version.version = this.version;
//				}
//				return data;
//			}
//		}
//
//		@Override
//		public List<StateNode> getChildren() {
//			synchronized (this) {
//				return Lists.<StateNode> newArrayList(children.values());
//			}
//		}
//
//		@Override
//		public String getPath() {
//			StringBuilder sb = new StringBuilder();
//			getPath(sb);
//			return sb.toString();
//		}
//
//		private void getPath(StringBuilder sb) {
//			if (parent != null) {
//				parent.getPath(sb);
//			}
//
//			sb.append("/");
//			sb.append(key);
//		}
//
//		@Override
//		public boolean create(ByteString data) {
//			synchronized (this) {
//				if (this.data != null) {
//					return false;
//				}
//				this.data = data;
//				this.version++;
//				notifyParent();
//				return true;
//			}
//		}
//
//		@Override
//		public boolean delete(Txn txn) throws ConcurrentUpdateException {
//			synchronized (this) {
//				if (ifVersion != null) {
//					if (this.version != ifVersion.version) {
//						throw new ConcurrentUpdateException();
//					}
//				}
//				if (this.data == null) {
//					return false;
//				}
//
//				this.data = null;
//				this.version = 0;
//				notifyParent();
//				return true;
//			}
//		}
//
//		@Override
//		public void update(ByteString data, Txn txn)
//				throws ConcurrentUpdateException {
//			synchronized (this) {
//				if (ifVersion != null) {
//					if (this.version != ifVersion.version) {
//						throw new ConcurrentUpdateException();
//					}
//				}
//
//				this.data = data;
//				this.version++;
//
//				notifyParent();
//			}
//		}
//
//		@Override
//		public boolean exists() {
//			synchronized (this) {
//				return this.data != null;
//			}
//		}
//
//		private void notifyParent() {
//			if (parent != null) {
//				parent.incrementChildChangeCount();
//			}
//		}
//
//		private void incrementChildChangeCount() {
//			synchronized (this) {
//				this.childChangeCount++;
//			}
//		}
//
//		@Override
//		public Long getChildrenChangeCount() throws StateStoreException {
//			synchronized (this) {
//				return this.childChangeCount;
//			}
//		}
//
//	}
//
//	final Map<String, InMemoryStateNode> roots = Maps.newHashMap();
//
//	@Override
//	public InMemoryStateNode getRoot(String id) {
//		synchronized (roots) {
//			InMemoryStateNode node = roots.get(id);
//			if (node == null) {
//				node = new InMemoryStateNode(null, id);
//				roots.put(id, node);
//			}
//			return node;
//		}
//	}
//
//	@Override
//	public IdProvider getIdProvider(String name) {
//		// TODO: This is easy to implement!
//		throw new UnsupportedOperationException();
//	}
//
// }
