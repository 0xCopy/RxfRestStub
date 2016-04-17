package relaxfactory;

/**
 * Created by jim on 4/16/16.
 */
public interface HttpServer {

  String getName();

  String getVersion();

  int getPort();

  void start() throws Exception;

  void shutdown();

}
