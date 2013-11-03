// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ImageFormat.proto

package io.fathom.cloud.protobuf;

public final class ImageFormat {
  private ImageFormat() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface EntryDataOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // optional int64 id = 1;
    /**
     * <code>optional int64 id = 1;</code>
     */
    boolean hasId();
    /**
     * <code>optional int64 id = 1;</code>
     */
    long getId();

    // optional string name = 2;
    /**
     * <code>optional string name = 2;</code>
     */
    boolean hasName();
    /**
     * <code>optional string name = 2;</code>
     */
    java.lang.String getName();
    /**
     * <code>optional string name = 2;</code>
     */
    com.google.protobuf.ByteString
        getNameBytes();

    // optional int64 mode = 3;
    /**
     * <code>optional int64 mode = 3;</code>
     */
    boolean hasMode();
    /**
     * <code>optional int64 mode = 3;</code>
     */
    long getMode();

    // optional int64 user = 4;
    /**
     * <code>optional int64 user = 4;</code>
     */
    boolean hasUser();
    /**
     * <code>optional int64 user = 4;</code>
     */
    long getUser();

    // optional int64 group = 5;
    /**
     * <code>optional int64 group = 5;</code>
     */
    boolean hasGroup();
    /**
     * <code>optional int64 group = 5;</code>
     */
    long getGroup();

    // optional bytes hash = 6;
    /**
     * <code>optional bytes hash = 6;</code>
     */
    boolean hasHash();
    /**
     * <code>optional bytes hash = 6;</code>
     */
    com.google.protobuf.ByteString getHash();

    // repeated int64 children = 7;
    /**
     * <code>repeated int64 children = 7;</code>
     */
    java.util.List<java.lang.Long> getChildrenList();
    /**
     * <code>repeated int64 children = 7;</code>
     */
    int getChildrenCount();
    /**
     * <code>repeated int64 children = 7;</code>
     */
    long getChildren(int index);
  }
  /**
   * Protobuf type {@code fathomcloud.protobuf.EntryData}
   */
  public static final class EntryData extends
      com.google.protobuf.GeneratedMessage
      implements EntryDataOrBuilder {
    // Use EntryData.newBuilder() to construct.
    private EntryData(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private EntryData(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final EntryData defaultInstance;
    public static EntryData getDefaultInstance() {
      return defaultInstance;
    }

    public EntryData getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private EntryData(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 8: {
              bitField0_ |= 0x00000001;
              id_ = input.readInt64();
              break;
            }
            case 18: {
              bitField0_ |= 0x00000002;
              name_ = input.readBytes();
              break;
            }
            case 24: {
              bitField0_ |= 0x00000004;
              mode_ = input.readInt64();
              break;
            }
            case 32: {
              bitField0_ |= 0x00000008;
              user_ = input.readInt64();
              break;
            }
            case 40: {
              bitField0_ |= 0x00000010;
              group_ = input.readInt64();
              break;
            }
            case 50: {
              bitField0_ |= 0x00000020;
              hash_ = input.readBytes();
              break;
            }
            case 56: {
              if (!((mutable_bitField0_ & 0x00000040) == 0x00000040)) {
                children_ = new java.util.ArrayList<java.lang.Long>();
                mutable_bitField0_ |= 0x00000040;
              }
              children_.add(input.readInt64());
              break;
            }
            case 58: {
              int length = input.readRawVarint32();
              int limit = input.pushLimit(length);
              if (!((mutable_bitField0_ & 0x00000040) == 0x00000040) && input.getBytesUntilLimit() > 0) {
                children_ = new java.util.ArrayList<java.lang.Long>();
                mutable_bitField0_ |= 0x00000040;
              }
              while (input.getBytesUntilLimit() > 0) {
                children_.add(input.readInt64());
              }
              input.popLimit(limit);
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000040) == 0x00000040)) {
          children_ = java.util.Collections.unmodifiableList(children_);
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.fathom.cloud.protobuf.ImageFormat.internal_static_fathomcloud_protobuf_EntryData_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.fathom.cloud.protobuf.ImageFormat.internal_static_fathomcloud_protobuf_EntryData_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.fathom.cloud.protobuf.ImageFormat.EntryData.class, io.fathom.cloud.protobuf.ImageFormat.EntryData.Builder.class);
    }

    public static com.google.protobuf.Parser<EntryData> PARSER =
        new com.google.protobuf.AbstractParser<EntryData>() {
      public EntryData parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new EntryData(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<EntryData> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // optional int64 id = 1;
    public static final int ID_FIELD_NUMBER = 1;
    private long id_;
    /**
     * <code>optional int64 id = 1;</code>
     */
    public boolean hasId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>optional int64 id = 1;</code>
     */
    public long getId() {
      return id_;
    }

    // optional string name = 2;
    public static final int NAME_FIELD_NUMBER = 2;
    private java.lang.Object name_;
    /**
     * <code>optional string name = 2;</code>
     */
    public boolean hasName() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>optional string name = 2;</code>
     */
    public java.lang.String getName() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          name_ = s;
        }
        return s;
      }
    }
    /**
     * <code>optional string name = 2;</code>
     */
    public com.google.protobuf.ByteString
        getNameBytes() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    // optional int64 mode = 3;
    public static final int MODE_FIELD_NUMBER = 3;
    private long mode_;
    /**
     * <code>optional int64 mode = 3;</code>
     */
    public boolean hasMode() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>optional int64 mode = 3;</code>
     */
    public long getMode() {
      return mode_;
    }

    // optional int64 user = 4;
    public static final int USER_FIELD_NUMBER = 4;
    private long user_;
    /**
     * <code>optional int64 user = 4;</code>
     */
    public boolean hasUser() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>optional int64 user = 4;</code>
     */
    public long getUser() {
      return user_;
    }

    // optional int64 group = 5;
    public static final int GROUP_FIELD_NUMBER = 5;
    private long group_;
    /**
     * <code>optional int64 group = 5;</code>
     */
    public boolean hasGroup() {
      return ((bitField0_ & 0x00000010) == 0x00000010);
    }
    /**
     * <code>optional int64 group = 5;</code>
     */
    public long getGroup() {
      return group_;
    }

    // optional bytes hash = 6;
    public static final int HASH_FIELD_NUMBER = 6;
    private com.google.protobuf.ByteString hash_;
    /**
     * <code>optional bytes hash = 6;</code>
     */
    public boolean hasHash() {
      return ((bitField0_ & 0x00000020) == 0x00000020);
    }
    /**
     * <code>optional bytes hash = 6;</code>
     */
    public com.google.protobuf.ByteString getHash() {
      return hash_;
    }

    // repeated int64 children = 7;
    public static final int CHILDREN_FIELD_NUMBER = 7;
    private java.util.List<java.lang.Long> children_;
    /**
     * <code>repeated int64 children = 7;</code>
     */
    public java.util.List<java.lang.Long>
        getChildrenList() {
      return children_;
    }
    /**
     * <code>repeated int64 children = 7;</code>
     */
    public int getChildrenCount() {
      return children_.size();
    }
    /**
     * <code>repeated int64 children = 7;</code>
     */
    public long getChildren(int index) {
      return children_.get(index);
    }

    private void initFields() {
      id_ = 0L;
      name_ = "";
      mode_ = 0L;
      user_ = 0L;
      group_ = 0L;
      hash_ = com.google.protobuf.ByteString.EMPTY;
      children_ = java.util.Collections.emptyList();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeInt64(1, id_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBytes(2, getNameBytes());
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeInt64(3, mode_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeInt64(4, user_);
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        output.writeInt64(5, group_);
      }
      if (((bitField0_ & 0x00000020) == 0x00000020)) {
        output.writeBytes(6, hash_);
      }
      for (int i = 0; i < children_.size(); i++) {
        output.writeInt64(7, children_.get(i));
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(1, id_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, getNameBytes());
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(3, mode_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(4, user_);
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(5, group_);
      }
      if (((bitField0_ & 0x00000020) == 0x00000020)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(6, hash_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < children_.size(); i++) {
          dataSize += com.google.protobuf.CodedOutputStream
            .computeInt64SizeNoTag(children_.get(i));
        }
        size += dataSize;
        size += 1 * getChildrenList().size();
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static io.fathom.cloud.protobuf.ImageFormat.EntryData parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(io.fathom.cloud.protobuf.ImageFormat.EntryData prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code fathomcloud.protobuf.EntryData}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements io.fathom.cloud.protobuf.ImageFormat.EntryDataOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return io.fathom.cloud.protobuf.ImageFormat.internal_static_fathomcloud_protobuf_EntryData_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return io.fathom.cloud.protobuf.ImageFormat.internal_static_fathomcloud_protobuf_EntryData_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                io.fathom.cloud.protobuf.ImageFormat.EntryData.class, io.fathom.cloud.protobuf.ImageFormat.EntryData.Builder.class);
      }

      // Construct using io.fathom.cloud.protobuf.ImageFormat.EntryData.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        id_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000001);
        name_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        mode_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000004);
        user_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000008);
        group_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000010);
        hash_ = com.google.protobuf.ByteString.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000020);
        children_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000040);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return io.fathom.cloud.protobuf.ImageFormat.internal_static_fathomcloud_protobuf_EntryData_descriptor;
      }

      public io.fathom.cloud.protobuf.ImageFormat.EntryData getDefaultInstanceForType() {
        return io.fathom.cloud.protobuf.ImageFormat.EntryData.getDefaultInstance();
      }

      public io.fathom.cloud.protobuf.ImageFormat.EntryData build() {
        io.fathom.cloud.protobuf.ImageFormat.EntryData result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public io.fathom.cloud.protobuf.ImageFormat.EntryData buildPartial() {
        io.fathom.cloud.protobuf.ImageFormat.EntryData result = new io.fathom.cloud.protobuf.ImageFormat.EntryData(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.id_ = id_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.name_ = name_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.mode_ = mode_;
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        result.user_ = user_;
        if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
          to_bitField0_ |= 0x00000010;
        }
        result.group_ = group_;
        if (((from_bitField0_ & 0x00000020) == 0x00000020)) {
          to_bitField0_ |= 0x00000020;
        }
        result.hash_ = hash_;
        if (((bitField0_ & 0x00000040) == 0x00000040)) {
          children_ = java.util.Collections.unmodifiableList(children_);
          bitField0_ = (bitField0_ & ~0x00000040);
        }
        result.children_ = children_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof io.fathom.cloud.protobuf.ImageFormat.EntryData) {
          return mergeFrom((io.fathom.cloud.protobuf.ImageFormat.EntryData)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(io.fathom.cloud.protobuf.ImageFormat.EntryData other) {
        if (other == io.fathom.cloud.protobuf.ImageFormat.EntryData.getDefaultInstance()) return this;
        if (other.hasId()) {
          setId(other.getId());
        }
        if (other.hasName()) {
          bitField0_ |= 0x00000002;
          name_ = other.name_;
          onChanged();
        }
        if (other.hasMode()) {
          setMode(other.getMode());
        }
        if (other.hasUser()) {
          setUser(other.getUser());
        }
        if (other.hasGroup()) {
          setGroup(other.getGroup());
        }
        if (other.hasHash()) {
          setHash(other.getHash());
        }
        if (!other.children_.isEmpty()) {
          if (children_.isEmpty()) {
            children_ = other.children_;
            bitField0_ = (bitField0_ & ~0x00000040);
          } else {
            ensureChildrenIsMutable();
            children_.addAll(other.children_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        io.fathom.cloud.protobuf.ImageFormat.EntryData parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (io.fathom.cloud.protobuf.ImageFormat.EntryData) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // optional int64 id = 1;
      private long id_ ;
      /**
       * <code>optional int64 id = 1;</code>
       */
      public boolean hasId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>optional int64 id = 1;</code>
       */
      public long getId() {
        return id_;
      }
      /**
       * <code>optional int64 id = 1;</code>
       */
      public Builder setId(long value) {
        bitField0_ |= 0x00000001;
        id_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int64 id = 1;</code>
       */
      public Builder clearId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        id_ = 0L;
        onChanged();
        return this;
      }

      // optional string name = 2;
      private java.lang.Object name_ = "";
      /**
       * <code>optional string name = 2;</code>
       */
      public boolean hasName() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>optional string name = 2;</code>
       */
      public java.lang.String getName() {
        java.lang.Object ref = name_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          name_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>optional string name = 2;</code>
       */
      public com.google.protobuf.ByteString
          getNameBytes() {
        java.lang.Object ref = name_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          name_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>optional string name = 2;</code>
       */
      public Builder setName(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        name_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional string name = 2;</code>
       */
      public Builder clearName() {
        bitField0_ = (bitField0_ & ~0x00000002);
        name_ = getDefaultInstance().getName();
        onChanged();
        return this;
      }
      /**
       * <code>optional string name = 2;</code>
       */
      public Builder setNameBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        name_ = value;
        onChanged();
        return this;
      }

      // optional int64 mode = 3;
      private long mode_ ;
      /**
       * <code>optional int64 mode = 3;</code>
       */
      public boolean hasMode() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>optional int64 mode = 3;</code>
       */
      public long getMode() {
        return mode_;
      }
      /**
       * <code>optional int64 mode = 3;</code>
       */
      public Builder setMode(long value) {
        bitField0_ |= 0x00000004;
        mode_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int64 mode = 3;</code>
       */
      public Builder clearMode() {
        bitField0_ = (bitField0_ & ~0x00000004);
        mode_ = 0L;
        onChanged();
        return this;
      }

      // optional int64 user = 4;
      private long user_ ;
      /**
       * <code>optional int64 user = 4;</code>
       */
      public boolean hasUser() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      /**
       * <code>optional int64 user = 4;</code>
       */
      public long getUser() {
        return user_;
      }
      /**
       * <code>optional int64 user = 4;</code>
       */
      public Builder setUser(long value) {
        bitField0_ |= 0x00000008;
        user_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int64 user = 4;</code>
       */
      public Builder clearUser() {
        bitField0_ = (bitField0_ & ~0x00000008);
        user_ = 0L;
        onChanged();
        return this;
      }

      // optional int64 group = 5;
      private long group_ ;
      /**
       * <code>optional int64 group = 5;</code>
       */
      public boolean hasGroup() {
        return ((bitField0_ & 0x00000010) == 0x00000010);
      }
      /**
       * <code>optional int64 group = 5;</code>
       */
      public long getGroup() {
        return group_;
      }
      /**
       * <code>optional int64 group = 5;</code>
       */
      public Builder setGroup(long value) {
        bitField0_ |= 0x00000010;
        group_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int64 group = 5;</code>
       */
      public Builder clearGroup() {
        bitField0_ = (bitField0_ & ~0x00000010);
        group_ = 0L;
        onChanged();
        return this;
      }

      // optional bytes hash = 6;
      private com.google.protobuf.ByteString hash_ = com.google.protobuf.ByteString.EMPTY;
      /**
       * <code>optional bytes hash = 6;</code>
       */
      public boolean hasHash() {
        return ((bitField0_ & 0x00000020) == 0x00000020);
      }
      /**
       * <code>optional bytes hash = 6;</code>
       */
      public com.google.protobuf.ByteString getHash() {
        return hash_;
      }
      /**
       * <code>optional bytes hash = 6;</code>
       */
      public Builder setHash(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000020;
        hash_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional bytes hash = 6;</code>
       */
      public Builder clearHash() {
        bitField0_ = (bitField0_ & ~0x00000020);
        hash_ = getDefaultInstance().getHash();
        onChanged();
        return this;
      }

      // repeated int64 children = 7;
      private java.util.List<java.lang.Long> children_ = java.util.Collections.emptyList();
      private void ensureChildrenIsMutable() {
        if (!((bitField0_ & 0x00000040) == 0x00000040)) {
          children_ = new java.util.ArrayList<java.lang.Long>(children_);
          bitField0_ |= 0x00000040;
         }
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public java.util.List<java.lang.Long>
          getChildrenList() {
        return java.util.Collections.unmodifiableList(children_);
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public int getChildrenCount() {
        return children_.size();
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public long getChildren(int index) {
        return children_.get(index);
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public Builder setChildren(
          int index, long value) {
        ensureChildrenIsMutable();
        children_.set(index, value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public Builder addChildren(long value) {
        ensureChildrenIsMutable();
        children_.add(value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public Builder addAllChildren(
          java.lang.Iterable<? extends java.lang.Long> values) {
        ensureChildrenIsMutable();
        super.addAll(values, children_);
        onChanged();
        return this;
      }
      /**
       * <code>repeated int64 children = 7;</code>
       */
      public Builder clearChildren() {
        children_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000040);
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:fathomcloud.protobuf.EntryData)
    }

    static {
      defaultInstance = new EntryData(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:fathomcloud.protobuf.EntryData)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_fathomcloud_protobuf_EntryData_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_fathomcloud_protobuf_EntryData_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021ImageFormat.proto\022\024fathomcloud.protobu" +
      "f\"p\n\tEntryData\022\n\n\002id\030\001 \001(\003\022\014\n\004name\030\002 \001(\t" +
      "\022\014\n\004mode\030\003 \001(\003\022\014\n\004user\030\004 \001(\003\022\r\n\005group\030\005 " +
      "\001(\003\022\014\n\004hash\030\006 \001(\014\022\020\n\010children\030\007 \003(\003B\032\n\030i" +
      "o.fathom.cloud.protobuf"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_fathomcloud_protobuf_EntryData_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_fathomcloud_protobuf_EntryData_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_fathomcloud_protobuf_EntryData_descriptor,
              new java.lang.String[] { "Id", "Name", "Mode", "User", "Group", "Hash", "Children", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}