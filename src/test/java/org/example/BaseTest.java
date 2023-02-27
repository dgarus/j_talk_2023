package org.example;

import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class BaseTest {

    static Random RANDOM = new Random();

    /**
     * Size of one ByteBuffer.
     */
    static int BUF_SIZE = 4 * 1024;

    /**
     * Number of ByteBuffer's.
     */
    static int BUF_NUM = 5;

    /**
     * @return Total number of bytes.
     */
    static int total() {
        return BUF_NUM * BUF_SIZE;
    }

    /**
     * Simulation of data processing
     *
     * @param input Input stream.
     * @param out Output stream.
     */
    void process(InputStream input, OutputStream out) {
        try {
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = input.read(buffer)) != -1) {
                System.out.printf("Process %d bytes\n", size);
                out.write(buffer, 0, size);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Publisher тестовых данных.
     */
    Publisher<ByteBuffer> generate() {
        return Flux.generate(() -> 0, (state, sink) -> {
                int i = state;
                if (i == BUF_NUM) {
                    sink.complete();
                } else {
                    byte[] data = new byte[BUF_SIZE];
                    RANDOM.nextBytes(data);
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    System.out.printf("Emitted bunch %d,  %d bytes\n", i, bb.remaining());
                    sink.next(bb);
                }
                return i + 1;
            }).delayElements(Duration.ofSeconds(2))
            .map(b -> (ByteBuffer) b);
    }

    /**
     * @return CompletableFuture returns ByteBuffer, that contains Publisher's all data.
     */
    CompletableFuture<ByteBuffer> reduceFuture(Publisher<ByteBuffer> source) {
        return Flowable.fromPublisher(source)
            .reduce(ByteBuffer.allocate(0),
                (left, right) -> {
                    right.mark();
                    final ByteBuffer result;
                    if (left.capacity() - left.limit() >= right.limit()) {
                        left.position(left.limit());
                        left.limit(left.limit() + right.limit());
                        result = left.put(right);
                    } else {
                        result = ByteBuffer.allocate(
                            2 * Math.max(left.capacity(), right.capacity())
                        ).put(left).put(right);
                    }
                    right.reset();
                    result.flip();
                    return result;
                }
            )
            .to(SingleInterop.get())
            .toCompletableFuture();
    }

}
