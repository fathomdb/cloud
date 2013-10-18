package io.fathom.cloud.state;

import javax.inject.Inject;

public class RepositoryBase {
    @Inject
    protected StateStore stateStore;

    // protected <T extends Builder, V> V deserialize(StateNode node,
    // Builder<T> builder) throws StateStoreException {
    // try {
    // return node.deserialize(builder);
    // } catch (IOException e) {
    // throw new StateStoreException("Error reading item: "
    // + node.getPath(), e);
    // }
    // }

    // protected <T> List<T> deserializeChildren(StateNode parent, Builder
    // builder)
    // throws CloudException {
    // List<T> items = Lists.newArrayList();
    // for (StateNode child : parent.getChildren()) {
    // try {
    // builder.clear();
    // T item = (T) child.deserialize(builder);
    // items.add(item);
    // } catch (IOException e) {
    // throw new IllegalStateException("Error reading item: "
    // + child.getPath(), e);
    // }
    // }
    // return items;
    // }

    // final Random random = new Random();
    //
    // synchronized int getRandom(int max) {
    // return random.nextInt(max);
    // }

    // protected Message putItem(StateNode parent, GeneratedMessage.Builder
    // builder, FieldDescriptor idField)
    // throws CloudException {
    // long id = ((Number) builder.getField(idField)).longValue();
    // boolean isCreate = false;
    //
    // if (id == 0) {
    // isCreate = true;
    // }
    //
    // while (true) {
    // if (isCreate) {
    // // Assign a new user id, randomly
    // id = getRandom(Integer.MAX_VALUE);
    // if (parent.hasChild(Long.toHexString(id))) {
    // continue;
    // }
    // builder.setField(idField, id);
    // }
    //
    // Message built = builder.build();
    //
    // ByteString data = built.toByteString();
    //
    // StateNode node = parent.child(Long.toHexString(id));
    //
    // if (isCreate) {
    // if (!node.create(data)) {
    // continue;
    // }
    // } else {
    // node.update(data);
    // }
    // return built;
    // }
    // }
}
