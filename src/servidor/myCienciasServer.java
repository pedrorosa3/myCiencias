package servidor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;


public class myCienciasServer {
	
    private static final int PORT = 23456;
    
    private final File usersFile = new File("users");
    private static final Scanner scanner = new Scanner(System.in);
    private static SecretKey macKey;
    private static final File usersMacFile = new File("users.mac");
    

    public static void main(String[] args) throws Exception {
        System.out.println("Servidor iniciado....");
        myCienciasServer server = new myCienciasServer();
        server.startServer();
    }
    
    
    private SecretKey deriveKeyFromPassword(String password) throws Exception {
        byte[] salt = "macsalt123456789".getBytes(StandardCharsets.UTF_8); // Usa um salt constante (poderia estar fixo ou guardado)
        int iterations = 65536;
        int keyLength = 256;
        
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec);
    }
    
    private byte[] computeMAC(File file, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                mac.update(buffer, 0, bytesRead);
            }
        }
        return mac.doFinal();
    }
    
    private boolean verifyMAC(File file, File macFile, SecretKey key) throws Exception {
        if (!macFile.exists()) {
            System.out.println("Aviso: MAC do ficheiro de passwords não existe.");
            return false;
        }
        byte[] actualMac = computeMAC(file, key);
        byte[] storedMac = Files.readAllBytes(macFile.toPath());
        return MessageDigest.isEqual(actualMac, storedMac);
    }
    
    private void updateMAC(File file, File macFile, SecretKey key) throws Exception {
        byte[] mac = computeMAC(file, key);
        Files.write(macFile.toPath(), mac);
    }
    
    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void createUserFileWithAdmin(String password) throws IOException {
        FileWriter writer = new FileWriter(usersFile);
        String salt = generateSalt();
        String hashed = hashPassword(password, salt);
        writer.write("admin:" + salt + ":" + hashed + "\n");
        writer.close();
    }

    public void startServer() throws Exception {
    	
        try{
        	
        	System.out.print("Introduza a password do utilizador admin: ");
            String adminPass = scanner.nextLine();
            macKey = deriveKeyFromPassword(adminPass);
            scanner.close();
        	
        	if (!usersFile.exists()) {
        		
                

                createUserFileWithAdmin(adminPass);
                updateMAC(usersFile, usersMacFile, macKey);
            }
        	else {
        		
        		if (!verifyMAC(usersFile, usersMacFile, macKey)) {
                    System.out.println("ERRO: O MAC do ficheiro de passwords não é válido. A terminar.");
                    System.exit(1);
                }
        	}
        	
        	System.out.println("Servidor iniciado com sucesso.");
        	
        	System.setProperty("javax.net.ssl.keyStore", "keystore.server");
        	System.setProperty("javax.net.ssl.keyStorePassword", "sigrupo14");
        	
        	ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
        	ServerSocket ss = ssf.createServerSocket(PORT);
        	
            while (true) {
            	
                //Socket clientSocket = serverSocket.accept();
            	
                //new ServerThread(clientSocket).start();
                new SSLSimpleServer(ss.accept()).start( );
            }
        } catch (IOException e) {
        	System.out.println("Erro ao iniciar o servidor.");
            //e.printStackTrace();
        }
    }
    
    private boolean authenticateUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].equals(username)) {
                    String salt = parts[1];
                    String expectedHash = parts[2];
                    String actualHash = hashPassword(password, salt);
                    return expectedHash.equals(actualHash);
                }
            }
        } catch (IOException e) {
        	System.out.println("Erro ao tentar verificar a autenticação do user");
            
        }
        return false;
    }
    
    private boolean userExists(String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader(usersFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(username + ":")) return true;
            }
        } catch (IOException e) {
        	System.out.println("Erro ao verificar se o user já existe");
            
        }
        return false;
    }
    
    private void createUser(String username, String password) throws Exception {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(usersFile, true))) {
            writer.write(username + ":" + salt + ":" + hash);
            writer.newLine();
        }

        File userDir = new File("server_storage/" + username);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        
        updateMAC(usersFile, usersMacFile, macKey);
    }



    // Thread para lidar com cada cliente
    public class SSLSimpleServer extends Thread {
    	private Socket socket = null;

		SSLSimpleServer(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

        public void run() {
            try {
            	
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                
                String username = (String) inStream.readObject();
                String password = (String) inStream.readObject();
                
                if(password.equals("criar")) {
                	System.out.println("A tentar criar utilizador...");
                	outStream.writeObject("a criar utilizador");
                }
                else {
                	
                	if (!authenticateUser(username, password)) {
                        outStream.writeObject("ERRO: Autenticação falhou");
                        System.out.println("ERRO: Autenticação falhou");
                        socket.close();
                        return;
                    }

                    outStream.writeObject("Autenticação bem-sucedida");
                	
                }
                
                
                
                String comando = (String) inStream.readObject();
                String userEmissor = (String) inStream.readObject();
                String userDest = (String) inStream.readObject();
                
                
                
                
                if(userDest==null) {
                	userDest = userEmissor;
                }
                
                if(comando.equals("-n")) {
                	System.out.println("Comando recebido: " + comando + " para o utilizador " + userEmissor);
                	
                }
                else {
                	System.out.println("Comando recebido: " + comando + " para o utilizador " + userDest);
                }
                
                
                outStream.writeObject("tá nice");
                
                
                
                
                if (comando.equals("-n")) {
                    String novoUsername = userEmissor;
                    String novaPassword = userDest;  // aqui o campo userDest está a ser usado para a password

                    if (userExists(novoUsername)) {
                    	System.out.println("Utilizador já existe");
                        outStream.writeObject("ERRO: Utilizador já existe");
                    } else {
                        try {
							createUser(novoUsername, novaPassword);
						} catch (Exception e) {
							System.out.println("Erro ao criar novo utilizador");
							
						}
                        outStream.writeObject("Utilizador criado com sucesso");
                        
                        
                        String useremissor33 =(String) inStream.readObject();
                        byte[] bytesize =(byte[]) inStream.readObject();
                        
                        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytesize);
                        Certificate cert = certFactory.generateCertificate(bais);

                        KeyStore serverKeystore = KeyStore.getInstance("PKCS12");
                        File serverKSFile = new File("certusers.keystore");
                        if (serverKSFile.exists()) {
                            try (FileInputStream fis = new FileInputStream(serverKSFile)) {
                                serverKeystore.load(fis, "sigrupo14".toCharArray());
                            } catch (NoSuchAlgorithmException e) {
                            	System.out.println("Erro ao aceder ao certificado dos users");
								
							}
                        } else {
                            serverKeystore.load(null, null);
                        }

                        // Guardar certificado no alias do utilizador
                        serverKeystore.setCertificateEntry(useremissor33, cert);

                        try (FileOutputStream fos = new FileOutputStream("certusers.keystore")) {
                            serverKeystore.store(fos, "sigrupo14".toCharArray());
                        }

                        System.out.println("Certificado do utilizador " + useremissor33 + " guardado na keystore do servidor.");
                        
                        outStream.writeObject("Certificado Guardado!");
                        
                        
                      
                    }

                    socket.close();
                    return;
                }
                else {
                	outStream.writeObject("continua que não é o comando -n");
                }

                
                
                
                
                File userDir = new File("server_storage/"+userDest);
                if (!userDir.exists()) {
                	userDir.mkdirs();
                    
                } else {
                    System.out.println("Diretório encontrado com sucesso.");
                }
               
           
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                if (comando.equals("-g")) {
                	
                	String rp33 = (String) inStream.readObject();
                	String rp34 = (String) inStream.readObject();
                	
                	if(rp33.equals("PEDIR_CERTIFICADO")) {
                		
                		KeyStore serverKeystore = KeyStore.getInstance("PKCS12");
                	    try (FileInputStream fis = new FileInputStream("certusers.keystore")) {
                	        serverKeystore.load(fis, "sigrupo14".toCharArray());
                	    }

                	    Certificate cert = serverKeystore.getCertificate(rp34);
                	    if (cert != null) {
                	    	outStream.writeObject("Tem certificado!");
                	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                	        ObjectOutputStream oos = new ObjectOutputStream(bos);
                	        oos.writeObject(cert.getEncoded());
                	        oos.flush();
                	        outStream.writeObject(bos.toByteArray());
                	    } else {
                	        outStream.writeObject("Certificado do utilizador: "+ rp34 +" não foi encontrado no servidor.");
                	        System.out.println("Certificado do utilizador: "+ rp34 +" não foi encontrado no servidor.");
                	        return;
                	       
                	    }
                	}
                	
                	
                	
                    sendFilesToClient(inStream, outStream, userDir,comando);
               
                } else {
                	
                	
                	String rp33 = (String) inStream.readObject();
                	String rp34 = (String) inStream.readObject();
                	
                	if(rp33.equals("PEDIR_CERTIFICADO")) {
                		
                		KeyStore serverKeystore = KeyStore.getInstance("PKCS12");
                	    try (FileInputStream fis = new FileInputStream("certusers.keystore")) {
                	        serverKeystore.load(fis, "sigrupo14".toCharArray());
                	    }

                	    Certificate cert = serverKeystore.getCertificate(rp34);
                	    if (cert != null) {
                	    	outStream.writeObject("Tem certificado!");
                	    	outStream.writeObject(cert.getEncoded());
                	    } else {
                	        outStream.writeObject("Certificado do utilizador: "+ rp34 +" não foi encontrado no servidor.");
                	        System.out.println("Certificado do utilizador: "+ rp34 +" não foi encontrado no servidor.");
                	        return;
                	       
                	    }
                	}
                	
                    receiveFilesFromClient(inStream, outStream, userDir);
                }

                outStream.close();
                inStream.close();
                socket.close();

            } catch (IOException | ClassNotFoundException e) {
            	System.out.println("O utilizador introduziu uma palavra passe da keystore errada.");
            } catch (CertificateException e) {
				
				e.printStackTrace();
			} catch (KeyStoreException e) {
				
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				
				e.printStackTrace();
			} 
        }
        
        public static String cortaNomeBase(String nomeFicheiro) {
            int primeiroPonto = nomeFicheiro.indexOf(".");
            int segundoPonto = nomeFicheiro.indexOf(".", primeiroPonto + 1);

            if (primeiroPonto != -1 && segundoPonto != -1) {
                return nomeFicheiro.substring(0, segundoPonto);
            } else {
                return nomeFicheiro;
            }
        }
        
        
        public static boolean contemFicheirosComNomeBase(File userDir, String nomeBase) {
            if (!userDir.exists() || !userDir.isDirectory()) {
                return false;
            }

            File[] ficheiros = userDir.listFiles((dir, name) -> name.startsWith(nomeBase));
            return ficheiros != null && ficheiros.length > 0;
        }

        /**
         * Método para receber ficheiros enviados pelo cliente
         */
        private void receiveFilesFromClient(ObjectInputStream inStream, ObjectOutputStream outStream, File userDir) throws IOException, ClassNotFoundException {
            while (true) {
                // Ler nome do ficheiro
            	
            	
            	
            	
                String fileName = (String) inStream.readObject();
                
                
                System.out.println(fileName);
                
                
                String nomeBase = cortaNomeBase(fileName);
                
                
                if (fileName.equals("Terminou")) {
                	System.out.println("Acabou");
                	break; // Sinal de fim
                }
                
                
                long dim = (long) inStream.readObject();
                
                Boolean flag = true;
                
                while(flag) {
                	
                	
                	
                	
                	
                		
                	if (contemFicheirosComNomeBase(userDir, nomeBase) && 
                			    (fileName.endsWith(".encrypted") || fileName.endsWith(".signed") || fileName.endsWith(".secure"))) {
                		
                		
                		
                		
                    	outStream.writeObject("já existe ficheiro com esse nome");
                    	outStream.writeObject("já existe ficheiro com esse nome");
                    	flag=false;
                    	continue;
                    }
                	else {
                		outStream.writeObject("Tudo bem");
                		outStream.writeObject("Tudo bem");
                	}
                    
                    
                    byte buffer[] = new byte[2048];
    				int n;
    				int jalidos=0;
    				
    				
    				File receivedFile = new File(userDir, fileName);
                    FileOutputStream f = new FileOutputStream(receivedFile);
    				
    				while((jalidos<dim)){
    					n=inStream.read(buffer,0,(dim-jalidos)>2048?2048:(int)(dim-jalidos));
    					
    					f.write(buffer,0,n);
    					jalidos+=n;
    				}
    				
    				

    				f.close();
    				
    				flag=false;
                	
                }
                
                
                
                
				
            }
            System.out.println("Todos os ficheiros foram recebidos com sucesso.");
            outStream.writeObject("Todos os ficheiros foram enviados com sucesso.");
            
        }

        /**
         * Método para enviar ficheiros solicitados pelo cliente
         */
        private void sendFilesToClient(ObjectInputStream inStream, ObjectOutputStream outStream, File userDir, String comando) throws IOException, ClassNotFoundException {
            while (true) {
                // Ler pedido do cliente
            	
                String requestedFileName = (String) inStream.readObject();
                
                
                String ultimaParte = requestedFileName.substring(requestedFileName.lastIndexOf('.') + 1);
                
                
                if (requestedFileName.equals("Terminou")) break;
                
                
                
                File requestedFile = new File(userDir+"/"+requestedFileName);
                System.out.println(requestedFile.getName());

                if (!requestedFile.exists()) {
                    outStream.writeObject("ERRO: Ficheiro não encontrado");
                    continue;
                }

                // Enviar confirmação e nome do ficheiro
                
                outStream.writeObject("OK");
                outStream.writeObject(requestedFile.getName());
                outStream.writeObject(requestedFile.length());
                
                String response33 = (String) inStream.readObject();
                
                if(response33.equals("já existe")) {
                	continue;
                }
                
                
                
             // Enviar conteúdo do ficheiro
                FileInputStream fis = new FileInputStream(requestedFile);
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = fis.read(buffer,0,2048)) > 0) {
                    outStream.write(buffer, 0, bytesRead);
                }
                outStream.flush();
                fis.close();

                
                System.out.println("Ficheiro enviado: " + requestedFile.getName());
                
                
             // Ler pedido do cliente
                
                
                
                
                // verificar se existe algum ficheiro e envia para o cliente o nome do assinante
                if(ultimaParte.equals("signed")) {
                	
                	String requestedFileName2 = (String) inStream.readObject();
                    
                    System.out.println(requestedFileName2);
                    
                    
                    if (requestedFileName2.equals("Terminou")) break;
                    
                    File[] ficheiros = userDir.listFiles((dirAtual, nome) -> nome.contains(requestedFileName2));
                    
                    String nomeuser = null;
                    
                    Boolean flag=false;
                    if(ficheiros.length>0) {
                    	nomeuser = ficheiros[0].getName().substring(ficheiros[0].getName().lastIndexOf('.') + 1);
                    	outStream.writeObject(nomeuser);
                    }
                    else {
                    	flag=true;
                    	outStream.writeObject("não tem o file.");
                    }
                    
                    
                    
                    if(flag) {
                    	
                    	outStream.writeObject("ERRO: Ficheiro não encontrado");
                        continue;
                    }
                    
                    File requestedFile2 = new File(userDir, requestedFileName2+nomeuser);

                    

                    // Enviar confirmação e nome do ficheiro
                    outStream.writeObject("OK");
                    outStream.writeObject(requestedFile2.getName());
                    outStream.writeObject(requestedFile2.length());
                    
                    
                    FileInputStream fis2 = new FileInputStream(requestedFile2);
                    byte[] buffer2 = new byte[2048];
                    int bytesRead2;
                    while ((bytesRead2 = fis2.read(buffer2,0,2048)) > 0) {
                        outStream.write(buffer2, 0, bytesRead2);
                    }
                    outStream.flush();
                    fis2.close();
                	
                }
                else if(ultimaParte.equals("encrypted")){
                	
                	
                	
                	String requestedFileName2 = (String) inStream.readObject();
                    
                    System.out.println(requestedFileName2);
                    
                    if (requestedFileName2.equals("Terminou")) break;
                    
                    File requestedFile2 = new File(userDir, requestedFileName2);
                    
                    if(!(requestedFile.exists())) {
                    	
                    	outStream.writeObject("ERRO: Ficheiro não encontrado");
                        continue;
                    }
                    
                    

                    

                    // Enviar confirmação e nome do ficheiro
                    outStream.writeObject("OK");
                    outStream.writeObject(requestedFile2.getName());
                    outStream.writeObject(requestedFile2.length());
                    
                    FileInputStream fis2 = new FileInputStream(requestedFile2);
                    byte[] buffer2 = new byte[2048];
                    int bytesRead2;
                    while ((bytesRead2 = fis2.read(buffer2,0,2048)) > 0) {
                        outStream.write(buffer2, 0, bytesRead2);
                    }
                    outStream.flush();
                    fis2.close();
                	
                	
                }
                else {
                	String requestedFileName2 = (String) inStream.readObject();
                    
                    System.out.println(requestedFileName2);
                    
                    if (requestedFileName2.equals("Terminou")) break;
                    
                    File requestedFile2 = new File(userDir, requestedFileName2);
                    
                    if(!(requestedFile.exists())) {
                    	
                    	outStream.writeObject("ERRO: Ficheiro não encontrado");
                        continue;
                    }
                    
                    

                    

                    // Enviar confirmação e nome do ficheiro
                    outStream.writeObject("OK");
                    outStream.writeObject(requestedFile2.getName());
                    outStream.writeObject(requestedFile2.length());
                    
                    
                    FileInputStream fis2 = new FileInputStream(requestedFile2);
                    byte[] buffer2 = new byte[2048];
                    int bytesRead2;
                    while ((bytesRead2 = fis2.read(buffer2,0,2048)) > 0) {
                        outStream.write(buffer2, 0, bytesRead2);
                    }
                    outStream.flush();
                    fis2.close();
                    
                    
                    String requestedFileName3 = (String) inStream.readObject();
                    
                    System.out.println(requestedFileName3);
                    
                    if (requestedFileName3.equals("Terminou")) break;
                    
                    File[] ficheiros = userDir.listFiles((dirAtual, nome) -> nome.contains(requestedFileName3));
                    
                    String nomeuser = null;
                    
                    Boolean flag=false;
                    if(ficheiros.length>0) {
                    	nomeuser = ficheiros[0].getName().substring(ficheiros[0].getName().lastIndexOf('.') + 1);
                    	outStream.writeObject(nomeuser);
                    }
                    else {
                    	flag=true;
                    	outStream.writeObject("não tem o file.");
                    }
                    
                    
                    
                    if(flag) {
                    	
                    	outStream.writeObject("ERRO: Ficheiro não encontrado");
                        continue;
                    }
                    
                    File requestedFile3 = new File(userDir, requestedFileName3+nomeuser);

                    

                    // Enviar confirmação e nome do ficheiro
                    outStream.writeObject("OK");
                    outStream.writeObject(requestedFile3.getName());
                    outStream.writeObject(requestedFile3.length());
                    
                    
                    
                    FileInputStream fis3 = new FileInputStream(requestedFile3);
                    byte[] buffer3 = new byte[2048];
                    int bytesRead3;
                    while ((bytesRead3 = fis3.read(buffer3,0,2048)) > 0) {
                        outStream.write(buffer3, 0, bytesRead3);
                    }
                    outStream.flush();
                    fis3.close();
                	
                	
                }
                
                
                
                
                
                
                
                
				
            }
            System.out.println("Todos os ficheiros foram enviados com sucesso!");
        }
    }
}
