package io.grpc.examples.canteiro;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente gRPC interativo do monitor corporativo de canteiros.
 */
public class CanteiroClient {

  // Oculta mensagens de debug do gRPC no terminal (ex.: Epoll, Netty).
  private static void silenciarLogsGrpc() {
    Logger.getLogger("io.grpc").setLevel(Level.WARNING);
    Logger.getLogger("io.grpc.netty").setLevel(Level.WARNING);
  }

  // Conecta ao servidor gRPC e abre o menu interativo ate o usuario sair.
  public static void main(String[] args) throws Exception {
    silenciarLogsGrpc();

    String target = "localhost:50052";
    if (args.length > 0 && "--help".equals(args[0])) {
      System.err.println("Uso: [target]");
      System.err.println("  target  Servidor gRPC. Padrao: " + target);
      System.exit(1);
    }
    if (args.length > 0) {
      target = args[0];
    }

    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    try (Scanner scanner = new Scanner(System.in)) {
      CanteiroMenu menu = new CanteiroMenu(CanteiroServiceGrpc.newBlockingStub(channel), scanner);
      menu.executar();
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
