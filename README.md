# Gestão de Frequências — Canteiro de Obras

**Disciplina:** Aplicações Distribuídas e Concorrentes (AD44S)  
**Curso:** Análise e Desenvolvimento de Sistemas — UTFPR Campus Pato Branco  
**Trabalho:** Sockets em Java: TCP/UDP (Trabalho III)  
**Acadêmico:** Matheus C. P. Santos — RA 2609380  
**Repositório:** https://github.com/mazeta1910/ADD44S-Gestao-de-Frequencias-Canteiro-de-Obras.git

---

## Sobre o projeto

Sistema distribuído para **gestão de frequência e recursos humanos em canteiros de obras**, alinhado ao ODS 8 (Trabalho Decente e Crescimento Econômico). O **servidor central** concentra o banco de dados e atende **terminais remotos** via **Sockets TCP**, permitindo registro de ponto e gestão corporativa a partir de outros computadores na rede.

## Arquitetura

```
┌─────────────────────────────┐         TCP (porta 8080)         ┌──────────────────────────────┐
│  TerminalCanteiroClienteTCP │  ◄────────────────────────────►  │     ServidorCentralTCP       │
│  (Nodo 2 — canteiro remoto) │                                  │  (Nodo 1 — servidor central) │
└─────────────────────────────┘                                  └──────────────┬───────────────┘
                                                                                 │
                                                                                 ▼
                                                                    PostgreSQL (trabalho_decente)
```

- **Nodo 1 — Servidor central:** autenticação, registro de ponto, persistência e execução remota do painel de gestão.
- **Nodo 2 — Terminal do canteiro:** interface para trabalhadores registrarem entrada, intervalo, retorno e saída.
- **Gestão remota:** administradores acessam o menu completo (`MenuConsoleSimplificado`) pelo terminal remoto; o processamento ocorre no servidor via `GestaoRemotaSession`.

## Tecnologias

- Java 21
- Sockets TCP (`ServerSocket` / `Socket`)
- JPA / Hibernate 6
- PostgreSQL
- Maven

## Principais classes

| Classe | Função |
|--------|--------|
| `ServidorCentralTCP` | Servidor TCP — escuta conexões e processa comandos |
| `TerminalCanteiroClienteTCP` | Cliente TCP — terminal de ponto no canteiro |
| `GestaoRemotaSession` | Encaminhamento remoto do menu de gestão |
| `NetworkConfig` | Configuração de IP, porta e codificação UTF-8 |
| `MenuConsoleSimplificado` | Painel de gestão (trabalhadores, canteiros, EPIs, relatórios) |
| `PopularBancoDados` | Popula o banco com dados de demonstração |

## Pré-requisitos

- JDK 21
- PostgreSQL com banco `trabalho_decente` (usuário/senha: `postgres`/`postgres`)
- Maven (opcional, se compilar pela linha de comando)

## Como executar

**1. Popular o banco (primeira vez):**
```text
PopularBancoDados
```

**2. No PC servidor — iniciar o servidor central:**
```text
ServidorCentralTCP
```

**3. No PC do canteiro — conectar ao servidor:**
```text
TerminalCanteiroClienteTCP [IP_DO_SERVIDOR] [PORTA]
```
Exemplo: `TerminalCanteiroClienteTCP 192.168.1.10 8080`

> Use o **IP da rede local** exibido pelo servidor, não `127.0.0.1`, quando conectar de outro computador. Libere a porta **8080** no firewall.

## Usuários de demonstração

Após executar `PopularBancoDados`, utilize estes CPFs para login no terminal:

| Perfil | Nome | CPF |
|--------|------|-----|
| Estagiário | Lucas Ferreira | `44444444444` |
| CLT | João Mestre de Obras | `22222222222` |
| Administrador | Matheus C. P. Santos | `11111111111` |

> O administrador tem acesso à **opção 7** (Painel de Gestão). Os demais perfis registram ponto normalmente.

## Funcionalidades

- Registro de ponto (entrada, intervalo, retorno, saída)
- Consulta de ficha de frequência e cálculo de jornada
- Gestão de trabalhadores, canteiros e EPIs
- Relatórios, exportação, backup e análise de desempenho (threads)
- Comunicação concorrente: uma thread por conexão TCP no servidor

## Por que TCP (e não UDP)?

O protocolo **TCP** foi escolhido porque o sistema trata **dados críticos de RH** (ponto, jornada, autenticação e persistência no PostgreSQL). Nesse cenário, a confiabilidade da comunicação importa mais do que a menor latência.

| Critério | TCP (escolhido) | UDP (não adequado aqui) |
|----------|-----------------|-------------------------|
| Entrega | Garante que os bytes chegam, na ordem correta | Não garante entrega nem ordem |
| Conexão | Canal persistente cliente ↔ servidor | Sem conexão; cada pacote é independente |
| Fluxo | Controle de fluxo e retransmissão automática | Sem controle; pacotes podem se perder |
| Uso típico | Transações, autenticação, menus interativos | Streaming, DNS, telemetria em tempo real |

**Motivos específicos deste projeto:**

1. **Registro de ponto não pode se perder** — uma batida de entrada ou saída perdida por UDP geraria inconsistência na folha de frequência.
2. **Sessão autenticada** — após `AUTH:CPF`, o servidor mantém o CPF na conexão e só aceita `CMD:` na mesma sessão; TCP mantém esse canal aberto de forma confiável.
3. **Diálogo cliente-servidor** — cada ação do menu envia um comando e espera uma resposta (`RESULTADO;...`, `FIM_FICHA`, `GESTAO_OK`). TCP é orientado a fluxo de bytes e combina com `readLine()` / `println()`.
4. **Gestão remota interativa** — o painel administrativo (`GestaoRemotaSession`) troca dezenas de linhas de entrada e saída pelo mesmo socket; perda ou desordem de pacotes quebraria o menu.
5. **Concorrência no servidor** — cada terminal remoto abre uma conexão TCP dedicada, atendida por uma thread (`accept()` + `new Thread(...)`), isolando as sessões sem misturar mensagens.

UDP seria mais indicado para cenários em que perda ocasional de dados é aceitável (ex.: sensor de temperatura no canteiro enviando leituras a cada segundo). Para **gestão de frequência e banco centralizado**, TCP é a escolha natural.

## Trechos do código (para explicação breve)

### 1. Servidor — escuta e concorrência (`ServidorCentralTCP`)

O servidor abre um `ServerSocket` na porta 8080 e, para cada terminal que conecta, cria uma **thread** dedicada:

```java
while (true) {
    Socket clientSocket = serverSocket.accept();
    new Thread(() -> processarRequisicao(clientSocket)).start();
}
```

Assim, vários canteiros podem registrar ponto ao mesmo tempo sem bloquear uns aos outros.

### 2. Protocolo de mensagens (texto por linha)

A comunicação usa **strings em UTF-8**, uma mensagem por linha:

| Prefixo | Exemplo | Significado |
|---------|---------|-------------|
| `AUTH:` | `AUTH:11111111111` | Cliente informa o CPF para login |
| `CMD:` | `CMD:PONTO_ENTRADA` | Comando após autenticação |
| Resposta | `AUTH_SUCCESS;ADMIN;Nome` | Servidor confirma perfil e nome |
| Resposta | `RESULTADO;Entrada registrada as 08:00` | Resultado de um comando de ponto |

No servidor, o loop principal lê linha a linha e roteia por prefixo:

```java
while ((mensagem = in.readLine()) != null) {
    if (mensagem.startsWith("AUTH:")) { /* valida CPF no banco */ }
    else if (mensagem.startsWith("CMD:") && cpfAutenticado != null) { /* executa comando */ }
}
```

### 3. Cliente — conexão e autenticação (`TerminalCanteiroClienteTCP`)

O terminal remoto abre o socket, envia o CPF e só entra no menu se o servidor responder `AUTH_SUCCESS`:

```java
Socket socket = new Socket(ipServidor, portaServidor);
out.println("AUTH:" + cpf);
String respostaAuth = in.readLine();
```

O IP e a porta vêm dos argumentos (`TerminalCanteiroClienteTCP 192.168.1.10 8080`) ou da classe `NetworkConfig`.

### 4. Registro de ponto no servidor

Cada comando (`PONTO_ENTRADA`, `PONTO_INTERVALO`, etc.) atualiza o `RegistroPonto` do dia no PostgreSQL via JPA e devolve confirmação ao cliente. O estado da jornada (`GET_ESTADO_JORNADA`) define quais opções o menu exibe no terminal.

### 5. Gestão remota (`GestaoRemotaSession`)

Quando o administrador escolhe a opção 7, o **processamento roda no servidor** (onde está o PostgreSQL), mas a **interface aparece no PC do canteiro**:

- Cliente envia `CMD:GESTAO_INICIAR`
- Servidor executa `MenuConsoleSimplificado` redirecionando `System.out` para o socket
- Entrada do teclado no cliente é reenviada pelo TCP ao servidor

Isso evita expor o banco de dados na rede local — apenas o servidor central precisa do PostgreSQL.

### 6. Configuração de rede (`NetworkConfig`)

Centraliza IP padrão (`127.0.0.1`), porta (`8080`), codificação UTF-8 nos streams e a descoberta dos IPs da máquina servidor.

**Como o servidor descobre o IP da máquina:** ao iniciar, `ServidorCentralTCP` chama `NetworkConfig.listarIpsLocais()`. Esse método percorre todas as **interfaces de rede** do PC com a API Java (`NetworkInterface.getNetworkInterfaces()`), ignora interfaces desligadas ou de loopback (`127.0.0.1`), e coleta apenas endereços **IPv4** ativos — por exemplo `192.168.1.10` da placa Wi-Fi ou Ethernet. Os IPs encontrados são impressos no console para o operador informar no terminal remoto. Se nenhum IP de rede for encontrado, o servidor sugere `127.0.0.1` (acesso só neste computador).

```java
Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
// Para cada interface ativa (não loopback):
//   coleta Inet4Address → ips.add(endereco.getHostAddress())
```

O **cliente**, por sua vez, não descobre o IP sozinho: ele recebe o IP do servidor por argumento (`TerminalCanteiroClienteTCP 192.168.1.10 8080`), variável de ambiente (`SERVIDOR_IP`) ou digitação manual no terminal.

## Passo a passo: do cliente ao servidor

Fluxo completo desde a execução dos programas até o registro de ponto, com os **trechos de código** onde cada etapa ocorre.

### Fase 1 — Servidor liga e fica aguardando

| Etapa | Onde | O que acontece |
|-------|------|----------------|
| 1 | `ServidorCentralTCP.main` | Resolve a porta (padrão **8080**) via `NetworkConfig` |
| 2 | `iniciarServidor` | Abre `ServerSocket` e fica em loop infinito escutando |
| 3 | `listarIpsLocais` | Lista IPs IPv4 da máquina (ex.: `192.168.1.10`) no console |
| 4 | `accept()` | Bloqueia até algum terminal remoto tentar conectar |

**Código — `ServidorCentralTCP.java`:**

```java
// main: resolve porta e chama iniciarServidor
int porta = NetworkConfig.resolverPortaServidor(args);
iniciarServidor(porta);

// iniciarServidor: abre ServerSocket, lista IPs e aguarda conexões
try (ServerSocket serverSocket = new ServerSocket(porta)) {
    List<String> ipsLocais = NetworkConfig.listarIpsLocais();
    // ... imprime IPs no console ...
    while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(() -> processarRequisicao(clientSocket)).start();
    }
}
```

**Código — descoberta de IP em `NetworkConfig.java`:**

```java
Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
while (interfaces.hasMoreElements()) {
    NetworkInterface iface = interfaces.nextElement();
    if (!iface.isUp() || iface.isLoopback()) continue;
    // coleta Inet4Address ativos → ips.add(endereco.getHostAddress())
}
```

### Fase 2 — Cliente inicia e abre a conexão TCP

| Etapa | Onde | O que acontece |
|-------|------|----------------|
| 1 | `TerminalCanteiroClienteTCP.main` | Exibe o banner do terminal |
| 2 | `NetworkConfig` | Obtém IP e porta (argumentos, env ou digitação do usuário) |
| 3 | Teclado | Usuário digita o **CPF** antes de conectar |
| 4 | `new Socket(ip, porta)` | Cliente inicia handshake TCP com o servidor |
| 5 | Servidor | `accept()` retorna um `Socket`; cria **nova thread** para esse cliente |
| 6 | Ambos | Abrem streams UTF-8: `BufferedReader` (leitura) e `PrintWriter` (escrita) |

**Código — cliente (`TerminalCanteiroClienteTCP.java`, linhas 20–47):**

```java
String ipServidor = NetworkConfig.resolverIpServidor(args);
int portaServidor = NetworkConfig.resolverPortaServidor(args);
// ... usuário pode digitar IP/porta se não passou por argumento ...

System.out.print("\nDigite seu CPF: ");
String cpf = scanner.nextLine();

try (
    Socket socket = new Socket(ipServidor, portaServidor);
    PrintWriter out = NetworkConfig.escritor(socket);
    BufferedReader in = NetworkConfig.leitor(socket)
) {
    // próxima fase: autenticação
}
```

**Código — servidor aceita e abre streams (`ServidorCentralTCP.java`, linhas 57–61):**

```java
private static void processarRequisicao(Socket clientSocket) {
    try (
        BufferedReader in = NetworkConfig.leitor(clientSocket);
        PrintWriter out = NetworkConfig.escritor(clientSocket)
    ) {
        // loop de mensagens...
    }
}
```

**Código — streams UTF-8 (`NetworkConfig.java`, linhas 123–137):**

```java
public static BufferedReader leitor(Socket socket) {
    return new BufferedReader(new InputStreamReader(
        socket.getInputStream(), StandardCharsets.UTF_8));
}
public static PrintWriter escritor(Socket socket) {
    return new PrintWriter(new OutputStreamWriter(
        socket.getOutputStream(), StandardCharsets.UTF_8), true);
}
```

### Fase 3 — Autenticação

| Etapa | Cliente | Servidor |
|-------|---------|----------|
| 1 | Envia `AUTH:` + CPF | Recebe a linha |
| 2 | Aguarda resposta | Busca trabalhador no banco |
| 3 | Se `AUTH_SUCCESS` → entra no menu | Define perfil (`ADMIN` ou `OPERACIONAL`) e nome |
| 4 | Se `AUTH_FAILED` → exibe erro e encerra | CPF inválido |

> Comandos `CMD:` só são aceitos **depois** de autenticação — veja a condição `cpfAutenticado != null` no servidor.

**Código — cliente envia CPF (`TerminalCanteiroClienteTCP.java`, linhas 49–61):**

```java
out.println("AUTH:" + cpf);
String respostaAuth = in.readLine();

if (respostaAuth != null && respostaAuth.startsWith("AUTH_SUCCESS")) {
    String[] partes = respostaAuth.split(";");
    boolean isAdmin = partes[1].equals("ADMIN");
    String nome = partes[2];
    System.out.println("\nBem-vindo, " + nome);
}
```

**Código — servidor valida no PostgreSQL (`ServidorCentralTCP.java`, linhas 66–82):**

```java
while ((mensagem = in.readLine()) != null) {
    if (mensagem.startsWith("AUTH:")) {
        cpfAutenticado = mensagem.substring(5);
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Trabalhador t = em.createQuery(
                "SELECT t FROM Trabalhador t WHERE t.cpf = :cpf", Trabalhador.class)
                .setParameter("cpf", cpfAutenticado).getSingleResult();

            if (t.isAdministrador()) {
                out.println("AUTH_SUCCESS;ADMIN;" + t.getNomeCompleto());
            } else {
                out.println("AUTH_SUCCESS;OPERACIONAL;" + t.getNomeCompleto());
            }
        } catch (NoResultException e) {
            out.println("AUTH_FAILED;CPF_INVALIDO");
        }
        em.close();
    }
```

### Fase 4 — Loop do menu (a cada volta)

| Etapa | Cliente | Servidor |
|-------|---------|----------|
| 1 | Envia `CMD:GET_ESTADO_JORNADA` | Consulta `RegistroPonto` do dia no banco |
| 2 | Recebe estado + meta + saldo | Calcula o estado da jornada |
| 3 | Monta menu conforme o estado | — |
| 4 | Usuário escolhe opção (1–7) | — |
| 5 | Envia `CMD:` correspondente | Executa comando e responde |
| 6 | Exibe resultado na tela | Persiste no PostgreSQL (`commit`) |

**Código — cliente pede estado e monta menu (`TerminalCanteiroClienteTCP.java`, linhas 63–103):**

```java
while (true) {
    out.println("CMD:GET_ESTADO_JORNADA");
    String[] respostaEstado = in.readLine().split(";");
    String estado = respostaEstado[1];

    if (estado.equals("AGUARDANDO_ENTRADA")) {
        System.out.println("1. Entrada");
    } else if (estado.equals("EM_TRABALHO")) {
        System.out.println("2. Saida para Intervalo");
        System.out.println("4. Saida (Fim do Expediente)");
    }
    // ... demais estados ...

    System.out.print("Opcao: ");
    String opcao = scanner.nextLine();
```

**Código — servidor calcula estado (`ServidorCentralTCP.java`, linhas 83–166):**

```java
} else if (mensagem.startsWith("CMD:") && cpfAutenticado != null) {
    String comando = mensagem.substring(4);

    EntityManager em = JPAUtil.getEntityManager();
    em.getTransaction().begin();
    // busca Trabalhador e RegistroPonto do dia (cria se não existir)

    if (comando.equals("GET_ESTADO_JORNADA")) {
        String estado;
        if (registroDiario.getHoraEntrada() == null) {
            estado = "AGUARDANDO_ENTRADA";
        } else if (registroDiario.getHoraSaidaIntervalo() == null
                && registroDiario.getHoraSaida() == null) {
            estado = "EM_TRABALHO";
        }
        // ... EM_INTERVALO, EM_TRABALHO_POS_INTERVALO, JORNADA_FINALIZADA ...
        out.println(JornadaUtil.montarRespostaEstado(estado, registroDiario, t, agora));
    }
```

### Fase 5 — Exemplo: registrar entrada (opção 1)

**Código — cliente envia comando (`TerminalCanteiroClienteTCP.java`, linhas 106–107 e 136–137):**

```java
if (opcao.equals("1") && estado.equals("AGUARDANDO_ENTRADA")) {
    out.println("CMD:PONTO_ENTRADA");
    opcaoValida = true;
}
// após envio, lê confirmação:
if (opcaoValida && !opcao.equals("5") && !opcao.equals("7")) {
    System.out.println("\n" + in.readLine().split(";")[1]);
}
```

**Código — servidor grava entrada (`ServidorCentralTCP.java`, linhas 132–134 e 198–200):**

```java
if (comando.equals("PONTO_ENTRADA")) {
    registroDiario.setHoraEntrada(agora);
    out.println("RESULTADO;Entrada registrada as " + agora.format(fmt));
}
// ao final de cada comando:
if (em.getTransaction().isActive()) {
    em.getTransaction().commit();
}
em.close();
```

### Fase 6 — Outras ações comuns

| Opção | Comando | Trecho no servidor |
|-------|---------|-------------------|
| 2 — Intervalo | `CMD:PONTO_INTERVALO` | `setHoraSaidaIntervalo(agora)` |
| 3 — Retorno | `CMD:PONTO_RETORNO` | `setHoraRetornoIntervalo(agora)` |
| 4 — Saída | `CMD:PONTO_SAIDA` | `setHoraSaida(agora)` + `JornadaUtil.calcularSaldo` |
| 5 — Ficha | `CMD:FICHA_FREQUENCIA` | loop `out.println(...)` + `FIM_FICHA` |
| 7 — Gestão | `CMD:GESTAO_INICIAR` | `GestaoRemotaSession.executarNoServidor` |

**Ficha de frequência — cliente lê várias linhas (`TerminalCanteiroClienteTCP.java`, linhas 114–120):**

```java
} else if (opcao.equals("5")) {
    out.println("CMD:FICHA_FREQUENCIA");
    String linha;
    while (!(linha = in.readLine()).equals("FIM_FICHA")) {
        System.out.println(linha);
    }
}
```

**Ficha — servidor monta tabela (`ServidorCentralTCP.java`, linhas 167–190):**

```java
} else if (comando.equals("FICHA_FREQUENCIA")) {
    List<RegistroPonto> ficha = em.createQuery(
        "SELECT r FROM RegistroPonto r WHERE r.trabalhador = :t ORDER BY r.dataRegistro DESC",
        RegistroPonto.class).setParameter("t", t).getResultList();
    // ... imprime linhas da tabela no socket ...
    out.println("FIM_FICHA");
}
```

**Gestão remota — cliente (`TerminalCanteiroClienteTCP.java`, linha 128):**

```java
} else if (opcao.equals("7") && isAdmin) {
    GestaoRemotaSession.executarNoCliente(out, in, scanner);
}
```

**Gestão remota — servidor (`ServidorCentralTCP.java`, linhas 86–101):**

```java
if (comando.equals("GESTAO_INICIAR")) {
    if (!admin.isAdministrador()) {
        out.println("GESTAO_ERRO;PERMISSAO_NEGADA");
    } else {
        out.println("GESTAO_OK");
        out.flush();
        GestaoRemotaSession.executarNoServidor(in, out);
        out.println("GESTAO_FIM");
        out.flush();
    }
    continue;
}
```

**Gestão remota — menu roda no servidor (`GestaoRemotaSession.java`, linhas 34–43):**

```java
public static void executarNoServidor(BufferedReader entradaRede, PrintWriter saidaRede) {
    System.setOut(saidaRedeStream);  // System.out → socket TCP
    MenuConsoleSimplificado.definirEntrada(new LinhaSocketInputStream(entradaRede));
    JPAUtil.configurarHostBanco("localhost");
    MenuConsoleSimplificado.exibirMenu();
}
```

### Fase 7 — Encerramento

| Etapa | Cliente | Servidor |
|-------|---------|----------|
| 1 | Opção 6 (Sair) | — |
| 2 | `CMD:SAIR` | Responde e encerra loop |
| 3 | Sai do `while` | `DESCONECTADO` + commit |
| 4 | `Socket` fecha (try-with-resources) | Thread termina |

**Código — cliente (`TerminalCanteiroClienteTCP.java`, linhas 121–123):**

```java
} else if (opcao.equals("6")) {
    out.println("CMD:SAIR");
    break;
}
```

**Código — servidor (`ServidorCentralTCP.java`, linhas 191–206):**

```java
} else if (comando.equals("SAIR")) {
    out.println("DESCONECTADO");
    em.getTransaction().commit();
    em.close();
    break;
}
// ...
} catch (Exception e) {
    System.out.println("Conexao encerrada.");
}
```

### Visão geral em sequência

```mermaid
sequenceDiagram
    participant C as TerminalCanteiroClienteTCP
    participant S as ServidorCentralTCP
    participant DB as PostgreSQL

    Note over S: Fase 1 — ServerSocket na porta 8080
    C->>S: TCP connect (Socket)
    S->>S: accept() + nova Thread

    C->>S: AUTH:CPF
    S->>DB: SELECT Trabalhador
    DB-->>S: dados do trabalhador
    S-->>C: AUTH_SUCCESS;perfil;nome

    loop Menu de ponto
        C->>S: CMD:GET_ESTADO_JORNADA
        S->>DB: SELECT RegistroPonto (hoje)
        DB-->>S: registro
        S-->>C: ESTADO;...;saldo
        C->>C: exibe menu
        C->>S: CMD:PONTO_* / FICHA / GESTAO / SAIR
        S->>DB: UPDATE / SELECT
        S-->>C: RESULTADO / FIM_FICHA / GESTAO_OK
    end

    C->>S: CMD:SAIR
    S-->>C: DESCONECTADO
```

---

*UTFPR — Campus Pato Branco — AD44S — 2026*
