package relaxfactory;

import bbcursive.Allocator;
import bbcursive.std;
 import one.xio.AsioVisitor.Impl;
import one.xio.AsyncSingletonServer;
import one.xio.HttpMethod;
import one.xio.HttpStatus;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bbcursive.Allocator.*;
import static bbcursive.Cursive.pre.*;
import static bbcursive.lib.Int.parseInt;
import static bbcursive.lib.log.log;
import static bbcursive.std.*;
 import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Helper.finishWrite;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.MimeType.text;

/**
 * Created by jim on 4/29/15.
 */
public class RxfHttpServer implements HttpServer {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    public static final int WIDE = Runtime.getRuntime().availableProcessors();
    public static final int PORT = Integer.parseInt(rxf.core.Config.get("PORT", "8888"));
    private ServerSocketChannel serverSocketChannel;


    private Impl protocoldecoder = new Impl() {
        public void onRead(SelectionKey key) throws Exception {

            Tx state;
            Object attachment = key.attachment();
            if (attachment instanceof Tx) {  //incomplete headers resuming
                state = Tx.acquireTx(key);
            } else {
                ByteBuffer byteBuffer;
                if (attachment instanceof ByteBuffer) {
                    byteBuffer = (ByteBuffer) attachment;
                } else byteBuffer = allocate(256);

                Tx tx = new Tx(key);
                (state = tx).hdr().headerBuf((ByteBuffer) byteBuffer.clear());
            }


            if (state.readHttpHeaders() && HttpMethod.GET == state.hdr().asRequest().httpMethod()) {




                HttpResponse httpResponse = state.hdr().asResponse();
                httpResponse.status(HttpStatus.$200).headerStrings().clear();
                ByteBuffer buffer = StandardCharsets.UTF_8.encode("enjoy your results");
                httpResponse.headerString(Content$2dType, text.contentType).headerString(Content$2dLength, String.valueOf(buffer.limit()));


                finishWrite(key, recycleThisKey -> {
                    recycleThisKey.interestOps(OP_READ).selector().wakeup();
                    recycleThisKey.attach(protocoldecoder);
                }, cat(state.hdr().asResponse().asByteBuffer(), bb(buffer, rewind)));


            }
        }
    };
    private int regCount = 0;
    private int port;

    public RxfHttpServer() {
        this(PORT);
    }

    public RxfHttpServer(int port) {
        this.port = port;
    }

    public String getName() {
        return "RelaxFactory";
    }

    public String getVersion() {
        return "almost.1";
    }

    public int getPort() {
        return PORT;
    }

    public void start() throws Exception {
        AsyncSingletonServer.killswitch.set(false);
        Shard[] shards = new Shard[WIDE];
        for (int i = 0; i < shards.length; i++) {
            Shard shard = new Shard();
            shards[i] = shard;
            executorService.execute(() -> {
                try {
                    shard.init(protocoldecoder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.yield( );
        Thread.yield( );
        Thread.yield( );
        serverSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().bind(new InetSocketAddress(   PORT) ).configureBlocking(false);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
         shards[regCount++ % shards.length].enqueue(serverSocketChannel, OP_ACCEPT, new Impl() {
            @Override
            public void onAccept(SelectionKey key) throws Exception {
                ServerSocketChannel c = (ServerSocketChannel) key.channel();
                SocketChannel newSocket = c.accept();
                newSocket.configureBlocking(false);
                shards[regCount++ % shards.length].enqueue(newSocket, OP_READ );

            }
        });
    }

    public void shutdown() {
        AsyncSingletonServer.killswitch.set(true);
        executorService.shutdownNow();
        try {
            serverSocketChannel.close();
        } catch (Throwable e) {

        }
        log("shutdown: "+this.getName()+" "+toString());
    }

    public static void main(String[] args)   {

        RxfHttpServer rxfHttpServer = new RxfHttpServer( PORT);
        try {
            rxfHttpServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int runtime = Runtime.getRuntime().availableProcessors();
        std.setAllocator( new Allocator(runtime * 4 * MEG));
        Object syncOn= new Object(){};
        synchronized (syncOn ){
            try {
            while(!AsyncSingletonServer.killswitch.get())    syncOn.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
