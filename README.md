# Gestão de Frequências — Canteiro de Obras

**Disciplina:** AD44S — Aplicações Distribuídas e Concorrentes  
**Acadêmico:** Matheus C. P. Santos — RA 2609380  
**UTFPR** Campus Pato Branco — Trabalho III (Sockets TCP)

---

## 1. Sobre o projeto

Sistema para **controlar a frequência dos trabalhadores em canteiros de obras**, alinhado ao ODS 8 (Trabalho Decente).

- **Servidor central** — concentra o banco PostgreSQL e processa tudo
- **Terminais nos canteiros** — interface para bater ponto pela rede
- **Comunicação** — Sockets TCP na porta 8080

---

## 2. Arquitetura

```
┌─────────────────────────────┐      TCP :8080       ┌──────────────────────────────┐
│  TerminalCanteiroClienteTCP │ ◄──────────────────► │     ServidorCentralTCP       │
│   (computador do canteiro)  │                      │   (computador do servidor)   │
└─────────────────────────────┘                      └──────────────┬───────────────┘
                                                                    ▼
                                                       PostgreSQL (trabalho_decente)
```

| Computador | Papel |
|------------|-------|
| Servidor central | Valida CPF, registra ponto, salva no banco, painel de gestão |
| Terminal do canteiro | Tela de ponto; admin acessa menu completo (opção 7) |

**Quem conecta em quem?**

O trabalhador usa o **PC do canteiro** (cliente), digita o CPF e escolhe as opções. Esse terminal **se conecta ao servidor central** — não o contrário.

```
Trabalhador → PC do canteiro → TCP → Servidor central → PostgreSQL
```

**Tecnologias:** Java 21 · Sockets TCP · JPA/Hibernate · PostgreSQL · Maven · gRPC (monitor corporativo)

---

## 2.1 Monitor corporativo (gRPC)

Complemento ao sistema de ponto: painel de gestão dos canteiros via **gRPC na porta 50052**.

| Canal | Porta | Função |
|-------|-------|--------|
| TCP | 8080 | Registro de ponto dos trabalhadores |
| gRPC | 50052 | Status, equipe, materiais, finanças e compras |

```powershell
cd monitor-grpc
.\gradlew.bat installDist
.\build\install\monitor-grpc\bin\canteiro-server.bat   # Terminal 1
.\build\install\monitor-grpc\bin\canteiro-client.bat   # Terminal 2
```

Detalhes em [monitor-grpc/README.md](monitor-grpc/README.md).

---

## 3. Por que TCP?

O registro de ponto precisa **chegar inteiro e na ordem certa**. TCP garante isso; UDP não.

- Cliente e servidor ficam **conectados** durante toda a sessão
- Cada ação envia um **pedido e espera resposta** (`RESULTADO`, `FIM_FICHA`)
- Vários terminais usam o sistema **ao mesmo tempo** (uma thread por conexão)
- UDP serviria para dados que podem se perder (ex.: sensor); **não para folha de ponto**

---

## 4. Principais classes

| Classe | O que faz |
|--------|-----------|
| `ServidorCentralTCP` | Escuta na porta 8080, processa comandos, grava no banco |
| `TerminalCanteiroClienteTCP` | Conecta ao servidor, faz login e exibe o menu |
| `NetworkConfig` | IP, porta e listagem dos IPs do servidor |
| `GestaoRemotaSession` | Painel admin no canteiro, processamento no servidor |
| `PopularBancoDados` | Cria dados de demonstração |

---

## 5. Threads

Cada **terminal conectado** ganha uma **thread** no servidor.

| Onde | Para quê |
|------|----------|
| `ServidorCentralTCP` | Uma thread por conexão TCP — terminais em paralelo |
| `GestaoRemotaSession` | Thread lê saída do servidor enquanto o usuário digita |
| `ProcessadorFolhaPagamento` | Cálculo paralelo da folha (menu admin → Análise de Desempenho) |

**Regra:** thread = conexão do terminal. Se cada canteiro tem 1 PC ligado, cada canteiro usa 1 thread.

**Exemplo — 3 funcionários, 3 PCs, mesmo canteiro:**

```
ServidorCentralTCP
├── Thread 1 → João  (CPF 22222222222)
├── Thread 2 → Lucas (CPF 44444444444)
└── Thread 3 → Maria (outro CPF)
```

Cada um faz login e bate ponto **em paralelo**, sem misturar registros. No **mesmo PC**, um usa por vez — sai (opção 6), o próximo conecta.

**No banco:** o `JPAUtil` usa `synchronized` para evitar conflito quando várias threads gravam juntas.

---

## 6. Demonstração — como rodar

```text
1. PopularBancoDados          # primeira vez
2. ServidorCentralTCP         # PC servidor — anote o IP exibido
3. TerminalCanteiroClienteTCP 192.168.x.x 8080   # PC do canteiro
```

> Use IP da rede local (`192.168.x.x`), não `127.0.0.1`, entre PCs diferentes. Libere a porta **8080** no firewall.

| Perfil | CPF | Observação |
|--------|-----|------------|
| Administrador | `11111111111` | Opção 7 — Painel de Gestão |
| CLT | `22222222222` | Registro de ponto |
| Estagiário | `44444444444` | Registro de ponto |

---

## 7. Fluxo da comunicação

```mermaid
sequenceDiagram
    participant C as Cliente
    participant S as Servidor
    participant DB as PostgreSQL

    C->>S: Conecta via TCP
    S->>S: Abre nova thread
    C->>S: Envia CPF
    S->>DB: Busca trabalhador
    S-->>C: Login aceito

    loop Menu de ponto
        C->>S: Pede estado da jornada
        S->>DB: Busca registro do dia
        S-->>C: Mostra situacao atual
        C->>S: Registra entrada (exemplo)
        S->>DB: Salva no banco
        S-->>C: Confirma registro
    end

    C->>S: Sair
    S-->>C: Desconectado
```

| # | Etapa |
|---|-------|
| 1 | Servidor liga na porta 8080 e aguarda conexão |
| 2 | Terminal conecta → servidor abre uma thread |
| 3 | Terminal envia CPF → servidor valida no banco |
| 4 | Terminal pede estado → servidor monta o menu |
| 5 | Trabalhador escolhe opção → servidor grava e confirma |
| 6 | Opção Sair → conexão encerra |

### Mensagens (uma linha por vez)

| Tipo | Exemplo |
|------|---------|
| Login | `AUTH:22222222222` |
| Comando | `CMD:PONTO_ENTRADA` |
| Login OK | `AUTH_SUCCESS;OPERACIONAL;João...` |
| Confirmação | `RESULTADO;Entrada registrada as 08:00` |

---

## 8. Código principal — Sockets TCP

A comunicação usa as classes `ServerSocket` e `Socket` do Java. O TCP abre um **canal persistente**: cliente e servidor trocam mensagens com `PrintWriter` (saída) e `BufferedReader` (entrada) ligados aos streams do socket.

**Servidor — `ServerSocket` escuta e `accept()` aceita a conexão TCP** (`ServidorCentralTCP.java`):

```java
// ServerSocket fica escutando a porta 8080 aguardando conexões TCP
try (ServerSocket serverSocket = new ServerSocket(porta)) {
    while (true) {
        // accept() bloqueia até um cliente conectar — retorna o Socket da sessão
        Socket clientSocket = serverSocket.accept();

        // Cada Socket TCP ganha uma thread — concorrência entre terminais
        new Thread(() -> processarRequisicao(clientSocket)).start();
    }
}
```

**Terminal — `Socket` conecta ao servidor e abre os streams TCP** (`TerminalCanteiroClienteTCP.java`):

```java
// Socket(ip, porta) inicia o handshake TCP com o servidor central
try (
    Socket socket = new Socket(ipServidor, portaServidor);
    PrintWriter out = NetworkConfig.escritor(socket);   // saída → socket.getOutputStream()
    BufferedReader in = NetworkConfig.leitor(socket)    // entrada ← socket.getInputStream()
) {
    // Envia uma linha pelo canal TCP (println adiciona \n no fim da mensagem)
    out.println("AUTH:" + cpf);

    // Bloqueia até o servidor responder pela mesma conexão TCP
    String respostaAuth = in.readLine();
}
```

**Servidor — leitura e escrita pelo canal TCP** (`ServidorCentralTCP.java`):

```java
// Streams criados a partir do Socket aceito — canal aberto com aquele terminal
BufferedReader in = NetworkConfig.leitor(clientSocket);
PrintWriter out = NetworkConfig.escritor(clientSocket);

// Lê linhas do socket enquanto a conexão TCP estiver ativa
while ((mensagem = in.readLine()) != null) {
    if (mensagem.startsWith("AUTH:")) {
        // Responde pelo mesmo socket TCP
        out.println("AUTH_SUCCESS;OPERACIONAL;" + nome);
    }
    else if (mensagem.startsWith("CMD:") && cpfAutenticado != null) {
        // Processa comando e devolve resultado pela conexão
        out.println("RESULTADO;Entrada registrada as 08:00");
    }
}
```

**IP do servidor** — `NetworkConfig.listarIpsLocais()` mostra o IP do PC (ex.: `192.168.1.10`) para o terminal saber onde abrir o `Socket`.

**Gestão remota (opção 7)** — o menu admin reutiliza o **mesmo Socket TCP**; saída e entrada trafegam pelo canal já aberto.

---

*UTFPR — Campus Pato Branco — AD44S — 2026*
