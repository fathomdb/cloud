package io.fathom.cloud.storage.api.os.resources;

import io.fathom.cloud.protobuf.FileModel.BucketData;

import java.io.IOException;
import java.util.List;

public class JsonBucketListWriter extends JsonWriter {

    private final List<BucketData> buckets;

    public JsonBucketListWriter(List<BucketData> buckets) {
        this.buckets = buckets;
    }

    @Override
    protected void write0() throws IOException {
        startArray();

        int count = 0;

        for (BucketData bucket : buckets) {
            if (count != 0) {
                writeComma();
            }
            count++;

            startObject();
            writeKeyLiteral("name");
            writeValue(bucket.getKey());
            // writeComma();
            // writeKey("count");
            // writeValue(bucket.getName());
            // writeComma();
            // writeKey("bytes");
            // writeValue(bucket.getName());
            endObject();
            // {"name":"test_container_1", "count":2, "bytes":78},
        }
        endArray();

    }

}
