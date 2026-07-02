package io.grpc.examples.canteiro;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

/**
 * Servidor gRPC do monitor corporativo de canteiros de obras.
 */
public class CanteiroServer {

  private Server server;

  private void start() throws IOException {
    int port = 50052;
    ExecutorService executor = Executors.newFixedThreadPool(4);
    server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
        .executor(executor)
        .addService(new CanteiroServiceImpl())
        .build()
        .start();
    System.out.println("INFO: Servidor de canteiros ativo na porta " + port);
    System.out.println("INFO: Dados em memoria - canteiros, equipe, materiais, financas e compras.");
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        System.err.println("*** encerrando servidor gRPC de canteiros ***");
        try {
          CanteiroServer.this.stop();
        } catch (InterruptedException e) {
          if (server != null) {
            server.shutdownNow();
          }
          e.printStackTrace(System.err);
        } finally {
          executor.shutdown();
        }
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    CanteiroServer server = new CanteiroServer();
    server.start();
    server.blockUntilShutdown();
  }

  private static final class CanteiroServiceImpl extends CanteiroServiceGrpc.CanteiroServiceImplBase {
    private final CanteiroRepository repository = new CanteiroRepository();

    @Override
    public void listCanteiros(
        ListarCanteirosRequest request, StreamObserver<ListarCanteirosReply> responseObserver) {
      responseObserver.onNext(repository.listarCanteiros());
      responseObserver.onCompleted();
    }

    @Override
    public void getStatus(StatusRequest request, StreamObserver<StatusReply> responseObserver) {
      responseObserver.onNext(repository.montarStatus(request.getCanteiroId(), LocalDateTime.now()));
      responseObserver.onCompleted();
    }

    @Override
    public void listFuncionarios(
        ListarFuncionariosRequest request, StreamObserver<ListarFuncionariosReply> responseObserver) {
      responseObserver.onNext(repository.listarFuncionarios(
          request.getTipo(), request.getCanteiroId(), LocalDateTime.now()));
      responseObserver.onCompleted();
    }

    @Override
    public void listMateriais(
        ListarMateriaisRequest request, StreamObserver<ListarMateriaisReply> responseObserver) {
      responseObserver.onNext(repository.listarMateriais(
          request.getCanteiroId(), request.getApenasBaixo()));
      responseObserver.onCompleted();
    }

    @Override
    public void listFinancas(
        ListarFinancasRequest request, StreamObserver<ListarFinancasReply> responseObserver) {
      responseObserver.onNext(repository.listarFinancas(
          request.getCanteiroId(), request.getApenasAlerta()));
      responseObserver.onCompleted();
    }

    @Override
    public void listCompras(
        ListarComprasRequest request, StreamObserver<ListarComprasReply> responseObserver) {
      responseObserver.onNext(repository.listarCompras(
          request.getCanteiroId(), request.getStatus()));
      responseObserver.onCompleted();
    }
  }
}
