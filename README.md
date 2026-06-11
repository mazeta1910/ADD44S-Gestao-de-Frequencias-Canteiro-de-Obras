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

---

*UTFPR — Campus Pato Branco — AD44S — 2026*
