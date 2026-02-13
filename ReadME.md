Servidor:
Executar:
java -cp src src/servidor/myCienciasServer.java

Cliente:
Compilar:
javac -cp "libs/*;src" -d src src/cliente/myCiencias.java src/criptUtils/utils.java

Executar:
java -cp "libs/*;src" src/cliente/myCiencias.java

Editar na linha 367 do myCiencias.java - caminho absoluto do/dos pdf que vão ser utilizados

pedro teste teste
tiago teste teste
tomas teste teste
joao teste teste

certusers.keystore sigrupo14

# myCiencias - Secure Document Storage 🛡️📂

## About the Project
**myCiencias** is a secure distributed application developed for the **Information Security** (Segurança Informática) course at the Faculty of Sciences, University of Lisbon (FCUL) during the 2024/2025 academic year.

The project implements a client-server system for storing and retrieving sensitive academic documents (such as certificates and diplomas). The primary focus was to build a "Security-by-Design" architecture capable of withstanding various adversarial capabilities, including network sniffing, message tampering, and replay attacks.

## System Architecture
The application is built in **Java** and operates over TCP Sockets. It evolved through two main development phases:

### Phase 1: Cryptographic Primitives
* **Hybrid Encryption:** Combines asymmetric encryption (RSA) for key exchange with symmetric encryption (AES/ChaCha20) for efficient file transfer.
* **Integrity:** Usage of Cryptographic Hash Functions (SHA-256) to verify file integrity.
* **Local Security:** Password-based encryption (PBE) to protect local keys and stored data.

### Phase 2: Secure Channels & Authentication
* **Transport Security:** Implementation of **TLS (Transport Layer Security)** to secure the communication channel between client and server, replacing raw TCP sockets.
* **Message Authentication:** Usage of **HMAC** (Hash-based Message Authentication Code) to ensure the authenticity and integrity of control messages.
* **Key Management:** Integration with **Java Keystores** to manage certificates and trusted authorities.
* **Anti-Replay:** Implementation of nonces/counters to prevent replay attacks.

## Security Features
* **Confidentiality:** All files are encrypted before storage and during transmission.
* **Integrity:** Detects any modification to the data in transit or at rest.
* **Authenticity:** Verifies the identity of both the user and the server using Digital Signatures and Certificates.
* **Forward Secrecy:** Achieved through ephemeral key exchanges within the TLS protocol.

## Threat Model
The system was designed to resist an adversary who can:
* Eavesdrop on the network (Sniffing).
* Intercept and modify packets (Man-in-the-Middle).
* Replay previously captured messages.
* Access the storage server (data remains encrypted at rest).

## Technologies
* **Language:** Java 17+
* **Networking:** Java Sockets, JSSE (Java Secure Socket Extension)
* **Cryptography:** JCA (Java Cryptography Architecture)
* **Algorithms:** AES-GCM, RSA, SHA-256, HMAC-SHA256, TLS 1.3

### Prerequisites
* Java Development Kit (JDK)
* Generated Keystores (Client and Server)
