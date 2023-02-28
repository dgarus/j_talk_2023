package org.example;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class RxJavaTest extends BaseTest {

    @Test
    void testRxJava() throws Exception {
        Publisher<ByteBuffer> byteBufferPublisher = dataSource();
        CompletableFuture<ByteBuffer> writeFuture;
        try (
            InputStream input = new PublisherAsInputStream(byteBufferPublisher, Schedulers.io()).inputStream();
            PublishingOutputStream output = new PublishingOutputStream()
        ) {
            writeFuture = dataConsumer(output.publisher());

            process(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int actual = writeFuture.get().remaining();
        System.out.printf("Total processed %d bytes\n", actual);
        assertEquals(actual, BaseTest.total());
    }

    static class PublisherAsInputStream extends DefaultSubscriber<ByteBuffer> {

        private final Publisher<ByteBuffer> publisher;

        private final Scheduler scheduler;

        private final PipedOutputStream out;

        private final PipedInputStream input;

        private final WritableByteChannel channel;

        PublisherAsInputStream(
            Publisher<ByteBuffer> publisher,
            Scheduler scheduler
        ) throws IOException {
            this.publisher = publisher;
            this.scheduler = scheduler;
            this.out = new PipedOutputStream();
            this.input = new PipedInputStream(this.out);
            this.channel = Channels.newChannel(this.out);
        }

        @Override
        public void onNext(final ByteBuffer buffer) {
            Objects.requireNonNull(buffer);
            try {
                while (buffer.hasRemaining()) {
                    System.out.printf("Writes %d bytes to input stream\n", buffer.remaining());
                    channel.write(buffer);
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public void onError(final Throwable err) {
            try {
                System.out.println("Close onError");
                input.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public void onComplete() {
            try {
                System.out.println("Close onComplete");
                out.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        InputStream inputStream() {
            Flowable.fromPublisher(publisher)
                .subscribeOn(scheduler)
                .subscribe(this);
            return input;
        }
    }

    static class PublishingOutputStream extends OutputStream {
        /**
         * Default period of time buffer collects bytes before it is emitted to publisher (ms).
         */
        private static final long DEFAULT_TIMESPAN = 100L;

        /**
         * Default maximum size of each buffer before it is emitted.
         */
        private static final int DEFAULT_BUF_SIZE = 512;

        /**
         * Resulting publisher.
         */
        private final UnicastProcessor<ByteBuffer> pub;

        /**
         * Buffer processor to collect bytes.
         */
        private final UnicastProcessor<Byte> bufProc;

        PublishingOutputStream() {
            this(
                PublishingOutputStream.DEFAULT_TIMESPAN,
                TimeUnit.MILLISECONDS,
                PublishingOutputStream.DEFAULT_BUF_SIZE
            );
        }

        PublishingOutputStream(
            final long timespan,
            final TimeUnit unit,
            final int count
        ) {
            this.pub = UnicastProcessor.create();
            this.bufProc = UnicastProcessor.create();
            this.bufProc.buffer(timespan, unit, count)
                .doOnNext(
                    list -> {
                        if (list.size() > 0) {
                            System.out.printf("Write %d to output publisher\n", list.size());
                        }
                        byte[] bytes = toByteArray(list);
                        this.pub.onNext(ByteBuffer.wrap(bytes));
                    }
                )
                .subscribeOn(Schedulers.io())
                .doOnComplete(this.pub::onComplete)
                .subscribe();
        }

        private byte[] toByteArray(List<Byte> list) {
            final byte[] result = new byte[list.size()];
            for (int i = 0; i < list.size(); i += 1) {
                result[i] = list.get(i);
            }
            return result;
        }

        @Override
        public void write(final int b) {
            bufProc.onNext((byte) b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            bufProc.onComplete();
        }

        Publisher<ByteBuffer> publisher() {
            return pub;
        }
    }
}
