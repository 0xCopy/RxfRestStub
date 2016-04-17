package relaxfactory;

import one.xio.AsyncSingletonServer;
import rxf.web.inf.ProtocolMethodDispatch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * Created by jim on 4/17/16.
 */
public class Server {


    public static final int PORT = Integer.parseInt(rxf.core.Config.get("PORT", "8888"));

    public static void main(String[] args) throws IOException {
        ProtocolMethodDispatch protocolMethodDispatch = new ProtocolMethodDispatch();
        AsyncSingletonServer.SingleThreadSingletonServer singleThreadSingletonServer = new AsyncSingletonServer.SingleThreadSingletonServer();
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().bind(new InetSocketAddress(PORT)).configureBlocking(false);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
            try {
                singleThreadSingletonServer.enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT );
                singleThreadSingletonServer.init(protocolMethodDispatch);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Object sync = new Object() {};

            synchronized (sync){
                    sync.wait();
                }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}
