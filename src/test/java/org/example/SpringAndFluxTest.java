package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;

public class SpringAndFluxTest extends BaseTest {

    @Test
    void testSpringAndFlux() throws Exception {
        Publisher<ByteBuffer> byteBufferPublisher = dataSource();

        try (InputStream input = inputStream(byteBufferPublisher)) {

            PipedOutputStream writeOsPipe = new PipedOutputStream();
            PipedInputStream writeIsPipe = new PipedInputStream(writeOsPipe);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> process(input, writeOsPipe))
                .thenRun(() ->
                    {
                        try {
                            writeOsPipe.close();
                        } catch (IOException e) {
                            System.out.println("Error closing Write Output Stream");
                        }
                    }
                );

            final Publisher<ByteBuffer> res = DataBufferUtils.readInputStream(
                () -> writeIsPipe, DefaultDataBufferFactory.sharedInstance, 512
            ).flatMapIterable(dataBuffer -> {
                System.out.printf("Write %d to output publisher\n", dataBuffer.readableByteCount());
                try (DataBuffer.ByteBufferIterator iter = dataBuffer.readableByteBuffers()) {
                    return (Iterable<ByteBuffer>) () -> iter;
                }
            });

            CompletableFuture<ByteBuffer> writeFuture = dataConsumer(res);
            future.get();

            int actual = writeFuture.get().remaining();
            System.out.printf("Total processed %d bytes\n", actual);
            assertEquals(actual, BaseTest.total());
        }
    }

    InputStream inputStream(Publisher<ByteBuffer> publisher) throws Exception {
        Publisher<DataBuffer> dataBufferPublisher = Flux.from(publisher)
            .map(DefaultDataBufferFactory.sharedInstance::wrap);

        PipedOutputStream osPipe = new PipedOutputStream();
        PipedInputStream isPipe = new PipedInputStream(osPipe);

        DataBufferUtils.write(dataBufferPublisher, osPipe).subscribe(
            b -> {
                // No-op
            },
            t -> {
                try {
                    isPipe.close();
                } catch (IOException ioe) {
                    System.out.println("Error closing streams: " + ioe);
                }
            },
            () -> {
                try {
                    osPipe.close();
                } catch (IOException ioe) {
                    System.out.println("Error closing streams: " + ioe);
                }
            }
        );
        return isPipe;
    }
}
