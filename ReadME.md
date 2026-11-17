<div align="center">

# 🛡️ myCiencias

### Armazenamento seguro de documentos académicos

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![TLS](https://img.shields.io/badge/TLS%2FSSL-seguro-blue)
![BouncyCastle](https://img.shields.io/badge/Bouncy%20Castle-1.80-green)
![FCUL](https://img.shields.io/badge/FCUL-Seguran%C3%A7a%20Inform%C3%A1tica-lightgrey)

</div>

---

| | |
|---|---|
| 👥 **Autores** | Pedro Rosa (60294) · Tiago Raposo (60293) · Tomás Nogueira (60285) — Grupo 14, LTI |
| 🏫 **Instituição** | Faculdade de Ciências da Universidade de Lisboa (FCUL) |
| 📚 **Unidade curricular** | Segurança Informática 2024/2025 |
| 📅 **Data de entrega** | Maio de 2025 |

---

## 📖 Sobre o projeto

O **myCiencias** é uma aplicação cliente-servidor para enviar, guardar e obter documentos académicos (certificados, diplomas, etc.) de forma segura. Cada utilizador pode **cifrar**, **assinar** ou **cifrar e assinar** ficheiros para outro utilizador. O destinatário vai depois buscá-los ao servidor, que os decifra e verifica as assinaturas.

O trabalho foi feito em duas fases:

- **1.ª fase:** cifra híbrida (AES + RSA), assinaturas digitais (SHA256withRSA) e envio de vários ficheiros de qualquer dimensão.
- **2.ª fase:** autenticação de utilizadores, passwords com *salt* + *hash*, MAC sobre o ficheiro de utilizadores, canal **TLS/SSL** e gestão automática de certificados.

## ✨ Funcionalidades de segurança

| Funcionalidade | Como é feita |
|---|---|
| 🔒 **Cifra híbrida** (`-c`) | Chave AES aleatória para o ficheiro, cifrada com a chave pública RSA do destinatário |
| ✍️ **Assinatura digital** (`-s`) | `SHA256withRSA` com a chave privada do emissor |
| 🔐 **Cifra + assinatura** (`-b`) | Assina primeiro e depois cifra (ficheiro `.secure`) |
| 🔑 **Passwords** | Guardadas como `user:salt:síntese` (SHA-256 com *salt*) no ficheiro `users` |
| ✅ **Integridade do `users`** | HMAC-SHA256 em `users.mac`, com chave derivada da password do admin (PBKDF2) |
| 🌐 **Canal seguro** | Sockets TLS/SSL com autenticação do servidor por certificado |
| 📜 **Certificados** | Keystore criada automaticamente para cada utilizador novo; certificados guardados no servidor em `certusers.keystore` |

## 🗂️ Estrutura

```
myCiencias/
├── src/
│   ├── cliente/myCiencias.java          # Cliente
│   ├── servidor/myCienciasServer.java   # Servidor (porta 23456)
│   └── criptUtils/utils.java            # Funções auxiliares de cifra/decifra
├── libs/                                # Bouncy Castle 1.80
├── keystore.server / cert.server        # Chave e certificado do servidor
├── truststore.client                    # Truststore do cliente
├── certusers.keystore                   # Certificados dos utilizadores (no servidor)
├── <user>.keystore                      # Keystore de cada utilizador
└── users / users.mac                    # Utilizadores registados + MAC
```

São gerados (e ignorados pelo git): `bin/` (classes compiladas), `server_storage/<user>/` (ficheiros guardados no servidor) e `Cliente_<user>/` (ficheiros recebidos pelo cliente).

## 🚀 Como executar

### Pré-requisitos

- **JDK 17** ou superior (testado com JDK 21)
- Um terminal aberto **na pasta raiz do projeto**

> [!NOTE]
> **Separador do classpath:** os comandos abaixo usam `;` (**Windows**). Em **Linux/macOS** use `:`, por exemplo `-cp "libs/*:bin"`.

### 0️⃣ Compilar

```bash
javac -encoding UTF-8 -cp "libs/*" -d bin src/cliente/myCiencias.java src/criptUtils/utils.java src/servidor/myCienciasServer.java
```

### 1️⃣ Iniciar o servidor

```bash
java -cp "libs/*;bin" servidor.myCienciasServer
```

O servidor pede a **password do admin**. Com os dados incluídos no repositório, a password é **`ola`**.

Para começar com um servidor **limpo**, apague `users`, `users.mac`, `certusers.keystore`, as keystores dos utilizadores (`<user>.keystore`) e a pasta `server_storage/`. Na primeira execução, a password que introduzir passa a ser a do `admin` e a chave do MAC.

Para descobrir o IP da máquina do servidor, use `ipconfig` (Windows) ou `ifconfig` (Linux). Na mesma máquina, use `localhost`.

### 2️⃣ Usar o cliente

As opções podem vir **por qualquer ordem**. A seguir a `-c`, `-s`, `-b` ou `-g` indica-se um ou mais ficheiros. Os ficheiros a enviar são indicados por caminho, relativo à pasta atual (ex.: `docs/diploma.pdf`) ou absoluto.

#### Registar um novo utilizador (`-n`)

```bash
java -cp "libs/*;bin" cliente.myCiencias -a <ip>:23456 -n <user> <password> -k <password-keystore>
```

Este comando cria a `<user>.keystore` e envia o certificado do utilizador para o servidor.

#### Enviar ficheiros: `-c` (cifrar), `-s` (assinar), `-b` (cifrar e assinar)

```bash
java -cp "libs/*;bin" cliente.myCiencias -a <ip>:23456 -e <user> -p <password> -k <password-keystore> -u <destinatario> -c <ficheiro1> [ficheiro2 ...]
java -cp "libs/*;bin" cliente.myCiencias -a <ip>:23456 -e <user> -p <password> -k <password-keystore> -u <destinatario> -s <ficheiro1> [ficheiro2 ...]
java -cp "libs/*;bin" cliente.myCiencias -a <ip>:23456 -e <user> -p <password> -k <password-keystore> -u <destinatario> -b <ficheiro1> [ficheiro2 ...]
```

Se o cliente ainda não tiver o certificado do destinatário, pede-o automaticamente ao servidor. Os ficheiros auxiliares criados durante o envio são apagados no fim.

#### Obter ficheiros (`-g`)

```bash
java -cp "libs/*;bin" cliente.myCiencias -a <ip>:23456 -e <user> -p <password> -k <password-keystore> -g <ficheiro1> [ficheiro2 ...]
```

O nome pode ser indicado sem extensão (`diploma.pdf`) ou com a extensão que tem no servidor:

| Enviado com | Nome no servidor | Resultado do `-g` |
|---|---|---|
| `-c` | `ficheiro.encrypted` | Ficheiro decifrado |
| `-s` | `ficheiro.signed` | Verificação da assinatura |
| `-b` | `ficheiro.secure` | Ficheiro decifrado + verificação da assinatura |

Os ficheiros recebidos ficam em `Cliente_<user>/`. Se o cliente não tiver o certificado de quem assinou, recebe-o automaticamente do servidor.

### 🧪 Utilizadores de teste

| Utilizador | Password | Password da keystore |
|---|---|---|
| `admin` (servidor) | `ola` | — |
| `pedro` | `teste` | `teste` |
| `tiago` | `teste` | `teste` |
| `tomas` | `teste` | `teste` |
| `joao`  | `teste` | `teste` |

Passwords fixas no código: `truststore.client`, `keystore.server` e `certusers.keystore` usam `sigrupo14`.

**Exemplo completo:**

```bash
# Servidor (password do admin: ola)
java -cp "libs/*;bin" servidor.myCienciasServer

# pedro envia ao tiago um diploma cifrado e assinado
java -cp "libs/*;bin" cliente.myCiencias -a localhost:23456 -e pedro -p teste -k teste -u tiago -b docs/diploma.pdf

# tiago obtém o diploma (decifra e verifica a assinatura do pedro)
java -cp "libs/*;bin" cliente.myCiencias -a localhost:23456 -e tiago -p teste -k teste -g diploma.pdf
```

### ⚠️ Comportamentos a ter em conta

- O servidor **recusa** um ficheiro se o destinatário já tiver um ficheiro com o mesmo nome base (`já existe ficheiro com esse nome`). Isto acontece mesmo que tenha sido enviado com outra opção, e os restantes ficheiros continuam a ser enviados.
- Uma password errada mostra `Password incorreta`. Um utilizador repetido no `-n` mostra `Utilizador com esse nome já existe`.
- Os ficheiros `users` e `users.mac` estão marcados como binários no [`.gitattributes`](.gitattributes). Assim o git não altera os fins de linha, o que invalidaria o MAC.

## 🔍 Análise crítica

| Fraqueza | Melhoria proposta |
|---|---|
| A password passada com `-p` fica visível no histórico e na lista de processos | Ler a password de forma segura (sem eco no terminal) |
| Com acesso ao ficheiro `users`, um atacante pode tentar ataques *offline* (força bruta/dicionário) às *hashes* | Limitar tentativas de autenticação e registar acessos em *logs* |

## 🛠️ Tecnologias

- **Linguagem:** Java
- **Rede:** Java Sockets, JSSE (TLS/SSL)
- **Criptografia:** JCA + Bouncy Castle 1.80
- **Algoritmos:** AES, RSA, SHA256withRSA, SHA-256, HMAC-SHA256, PBKDF2

## 📄 Relatório

O relatório da 2.ª fase está em [`Relatório SI - Grupo 14 - 2ª Fase.pdf`](Relatório%20SI%20-%20Grupo%2014%20-%202ª%20Fase.pdf).

---

<div align="center">
Grupo 14 · Pedro Rosa · Tiago Raposo · Tomás Nogueira · FCUL · Maio de 2025
</div>
