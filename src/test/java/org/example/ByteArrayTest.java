package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;

public class ByteArrayTest extends BaseTest {

    @Test
    void testByteArray() throws Exception {
        Publisher<ByteBuffer> byteBufferPublisher = dataSource();
        ByteBuffer bb = reduce(byteBufferPublisher).get();
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        try (
            InputStream input = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length)
        ) {
            process(input, output);

            int actual = reduce(
                Flux.fromArray(new ByteBuffer[]{ByteBuffer.wrap(output.toByteArray())})
            ).get().remaining();
            System.out.printf("Total processed %d bytes\n", actual);
            assertEquals(actual, BaseTest.total());
        }
    }

}
