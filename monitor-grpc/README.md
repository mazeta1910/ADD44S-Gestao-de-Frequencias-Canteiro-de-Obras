# Monitor gRPC — Canteiros de Obras

Modulo complementar ao sistema TCP de ponto. Expoe painel corporativo via gRPC na porta **50052**.

## Como rodar (Windows)

```powershell
cd monitor-grpc
.\gradlew.bat installDist

# Terminal 1
.\build\install\monitor-grpc\bin\canteiro-server.bat

# Terminal 2
.\build\install\monitor-grpc\bin\canteiro-client.bat
```

## Menu principal

1. Canteiros — status operacional em tempo real
2. Funcionarios — equipe por tipo e por canteiro
3. Materiais — estoque e alertas
4. Financas — orcamento, gastos e saldo
5. Compras — pedidos e aprovacoes

Dados em memoria (demonstracao), sem dependencia do PostgreSQL.
