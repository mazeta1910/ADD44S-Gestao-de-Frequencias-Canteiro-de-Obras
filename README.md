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

## Funcionalidades

- Registro de ponto (entrada, intervalo, retorno, saída)
- Consulta de ficha de frequência e cálculo de jornada
- Gestão de trabalhadores, canteiros e EPIs
- Relatórios, exportação, backup e análise de desempenho (threads)
- Comunicação concorrente: uma thread por conexão TCP no servidor

---

*UTFPR — Campus Pato Branco — AD44S — 2026*
