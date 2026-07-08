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

  // Inicia o servidor na porta 50052 e registra encerramento ao fechar a JVM.
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

  // Encerra o servidor de forma ordenada, aguardando ate 30 segundos.
  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  // Mantem o processo principal vivo enquanto o servidor estiver ativo.
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  // Ponto de entrada: sobe o servidor e bloqueia ate ser encerrado.
  public static void main(String[] args) throws IOException, InterruptedException {
    CanteiroServer server = new CanteiroServer();
    server.start();
    server.blockUntilShutdown();
  }

  // Implementacao dos RPCs definidos em canteiro.proto.
  private static final class CanteiroServiceImpl extends CanteiroServiceGrpc.CanteiroServiceImplBase {
    private final CanteiroRepository repository = new CanteiroRepository();

    // RPC: retorna a lista de canteiros cadastrados em memoria.
    @Override
    public void listCanteiros(
        ListarCanteirosRequest request, StreamObserver<ListarCanteirosReply> responseObserver) {
      responseObserver.onNext(repository.listarCanteiros());
      responseObserver.onCompleted();
    }

    // RPC: monta o status operacional do canteiro no momento da consulta.
    @Override
    public void getStatus(StatusRequest request, StreamObserver<StatusReply> responseObserver) {
      responseObserver.onNext(repository.montarStatus(request.getCanteiroId(), LocalDateTime.now()));
      responseObserver.onCompleted();
    }

    // RPC: lista funcionarios filtrados por tipo e/ou canteiro.
    @Override
    public void listFuncionarios(
        ListarFuncionariosRequest request, StreamObserver<ListarFuncionariosReply> responseObserver) {
      responseObserver.onNext(repository.listarFuncionarios(
          request.getTipo(), request.getCanteiroId(), LocalDateTime.now()));
      responseObserver.onCompleted();
    }

    // RPC: lista materiais em estoque, com opcao de filtrar estoque baixo.
    @Override
    public void listMateriais(
        ListarMateriaisRequest request, StreamObserver<ListarMateriaisReply> responseObserver) {
      responseObserver.onNext(repository.listarMateriais(
          request.getCanteiroId(), request.getApenasBaixo()));
      responseObserver.onCompleted();
    }

    // RPC: retorna dados financeiros do canteiro (orcamento, gastos, saldo).
    @Override
    public void listFinancas(
        ListarFinancasRequest request, StreamObserver<ListarFinancasReply> responseObserver) {
      responseObserver.onNext(repository.listarFinancas(
          request.getCanteiroId(), request.getApenasAlerta()));
      responseObserver.onCompleted();
    }

    // RPC: lista pedidos de compra filtrados por canteiro e status.
    @Override
    public void listCompras(
        ListarComprasRequest request, StreamObserver<ListarComprasReply> responseObserver) {
      responseObserver.onNext(repository.listarCompras(
          request.getCanteiroId(), request.getStatus()));
      responseObserver.onCompleted();
    }
  }
}
