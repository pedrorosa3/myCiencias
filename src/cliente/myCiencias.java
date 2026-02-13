package cliente;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import criptUtils.utils;

public class myCiencias {
	public static void main(String[] args) throws Exception {
		
		
		System.out.println("Cliente: Grupo14");
		myCiencias client = new myCiencias();
		
		if (args.length < 3) {
            System.out.println("Uso: java myCiencias -a <serverAddress> -u <user-do-emissor> -e <user-destinatario> <comando> <nome-ficheiro>");
            
        }
		else {
			String serverAddress = args[1];
			
	        String[] serversplit = serverAddress.split(":");
	        String ip = serversplit[0];
	        int port = Integer.parseInt(serversplit[1]);
	        String userEmissor = args[3];
	        String comandonewuser=args[2];
	        String userDestinatario = null;
	        String comando = null;
	        String[] ficheirossplit;
	        String nomesFicheiros;
	        String comandodecript;
	        String passwd;
	        String passwdkey;
	        if(comandonewuser.equals("-n")) {
	        	userDestinatario = args[4];
	        	comando=args[2];
	        	ficheirossplit=null;
	        	nomesFicheiros=null;
	        	comandodecript="nada";
	        	passwd="criar";
	        	passwdkey=args[6];
	        	
	        }
	        
	        
	        
	        
	        
	        
	        else {
	        	
	        	comandodecript = args[8];
		        passwd = args[5];
		        passwdkey = args[7];
		        
	        	if(comandodecript.equals("-g")) {
		        	
		        	StringBuilder nomesFicheiros1 = new StringBuilder();
			        for (int i = 9; i < args.length; i++) {
			            nomesFicheiros1.append(args[i]).append(" ");
			        }
			        ficheirossplit = nomesFicheiros1.toString().trim().split(" ");
			        
			        nomesFicheiros = nomesFicheiros1.toString();
			        
			        userDestinatario = userEmissor;
			        
		        }
		        else {
		        	userDestinatario = args[9];
			        comando = args[10];
			        StringBuilder nomesFicheiros1 = new StringBuilder();
			        for (int i = 11; i < args.length; i++) {
			            nomesFicheiros1.append(args[i]).append(" ");
			        }
			        ficheirossplit = nomesFicheiros1.toString().trim().split(" ");
			        
			        nomesFicheiros = nomesFicheiros1.toString();
		        }
	        	
	        }
	        
	        
			client.startConnection(serverAddress,serversplit,ip,port,userEmissor,userDestinatario,comando,nomesFicheiros,ficheirossplit,comandodecript,passwd,passwdkey);
		}
		
		
		
	}
	
	
	
	
	public void startConnection(String serverAddress, String[] serversplit, String ip, int port, String userEmissor, String userDestinatario, String comando, String nomesFicheiros, String[] ficheirossplit,String comandodecript,String passwd,String passwdkey) throws Exception {
	    
		
		
		System.setProperty("javax.net.ssl.trustStore", "truststore.client");
		System.setProperty("javax.net.ssl.trustStorePassword", "sigrupo14");
		
		SocketFactory sf = SSLSocketFactory.getDefault( );
		Socket s = sf.createSocket(ip, port);
	    
	    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
	    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
	    
	    if(comandodecript.equals("-g")) {
	    	comando=comandodecript;
	    }
	    
	    out.writeObject(userEmissor);
	    out.writeObject(passwd);
	    
	    String responseServer1 = (String) in.readObject();
	    if(responseServer1.equals("ERRO: Autenticação falhou")) {
	    	
	    	System.out.println("Password incorreta");
	    	s.close();
	    	return;
	    	
	    }
	    
	    System.out.println(comando);
	    out.writeObject(comando);
	    out.writeObject(userEmissor);
	    out.writeObject(userDestinatario);

	    String serverResponse = (String) in.readObject();
	    
	    System.out.println("Resposta do servidor1: " + serverResponse);
	    
	    String serverResponse34 = (String) in.readObject();
	    
	    if(serverResponse34.equals("ERRO: Utilizador já existe")) {
	    	System.out.println("Utilizador com esse nome já existe");
	    	return;
	    }
	    
	    if(serverResponse34.equals("Utilizador criado com sucesso")) {
	    	
	    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            X500Principal dnName = new X500Principal("CN=User, O=MyOrg, L=Lisbon, C=PT");
            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            Date endDate = new Date(now + (365L * 24 * 60 * 60 * 1000)); // 1 ano
            BigInteger certSerialNumber = BigInteger.valueOf(now);
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());

            
            
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            	    dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic());
            
            
            Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build((ContentSigner)contentSigner));
            // Criar keystore
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, null);
            
            
         // Guardar chave privada + certificado na keystore
            KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(
                    keyPair.getPrivate(), new java.security.cert.Certificate[]{certificate});
            KeyStore.PasswordProtection protParam = new KeyStore.PasswordProtection(passwdkey.toCharArray());
            keystore.setEntry(userEmissor, entry, protParam);
            
            
            try (FileOutputStream fos = new FileOutputStream(userEmissor+".keystore")) {
                keystore.store(fos, passwdkey.toCharArray());
            }
            
            // Carregar a keystore local
            KeyStore keystore334 = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(userEmissor + ".keystore")) {
                keystore334.load(fis, passwdkey.toCharArray());
            }

            // Obter o certificado
            Certificate cert = keystore.getCertificate(userEmissor);

            // Exportar certificado em bytes
            byte[] certBytes = cert.getEncoded();

            // Enviar para o servidor
              
            out.writeObject(userEmissor);    // Nome do utilizador
            out.writeObject(certBytes);      // Bytes do certificado
            out.flush();
            System.out.println("Certificado enviado para o servidor.");
            
            String responseCert = (String) in.readObject();
            System.out.println(responseCert);
            
            return;
            
            

            
	    }
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    

	    if (comando.equals("-g")) {
	    	
	    	KeyStore userKeystore = KeyStore.getInstance("PKCS12");
	    	try (FileInputStream fis = new FileInputStream(userEmissor + ".keystore")) {
	    	    userKeystore.load(fis, passwdkey.toCharArray());
	    	}
	    	catch(IOException e) {
	    		System.out.println("Password da keystore errada!");
	    		
	    		return;
	    	}

	    	if (!userKeystore.containsAlias(userDestinatario)) {
	    	    // Pedir ao servidor o certificado
	    	    out.writeObject("PEDIR_CERTIFICADO");
	    	    out.writeObject(userDestinatario);
	    	    
	    	    
	    	    String rp567 = (String) in.readObject();
	    	    
	    	    if(rp567.equals("Tem certificado!")) {
	    	    	
	    	    	// Receber o certificado do servidor
		    	    byte[] certBytes = (byte[]) in.readObject();
		    	    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
		    	    CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    	    Certificate receivedCert = cf.generateCertificate(bis);

		    	    // Adicionar à keystore local
		    	    userKeystore.setCertificateEntry(userDestinatario, receivedCert);
		    	    try (FileOutputStream fos = new FileOutputStream(userEmissor + ".keystore")) {
		    	        userKeystore.store(fos, passwdkey.toCharArray());
		    	    }
		    	    catch(IOException e) {
		    	    	System.out.println("Password da keystore errada!");
			    		
			    		return;
			    	}
	    	    	
	    	    }
	    	    else {
	    	    	System.out.println(rp567);
	    	    	return;
	    	    }
	    	    
	    	    
	    	    
	    	}
	    	else {
	    		out.writeObject("já tem");
	    		out.writeObject("já tem certificado");
	    	}
	    	
	    	
	        receiveFiles(out, in, userEmissor, ficheirossplit,passwdkey);
	        
	        
	    } else {
	    	
	    	KeyStore userKeystore = KeyStore.getInstance("PKCS12");
	    	try (FileInputStream fis = new FileInputStream(userEmissor + ".keystore")) {
	    	    userKeystore.load(fis, passwdkey.toCharArray());
	    	}
	    	catch(IOException e) {
	    		
	    		System.out.println("Password da keystore errada");
	    		return;
	    	}

	    	if (!userKeystore.containsAlias(userDestinatario)) {
	    		
	    		
	    	    // Pedir ao servidor o certificado
	    	    out.writeObject("PEDIR_CERTIFICADO");
	    	    out.writeObject(userDestinatario);
	    	    
	    	    
	    	    String rp567 = (String) in.readObject();
	    	    
	    	    if(rp567.equals("Tem certificado!")) {
	    	    	
	    	    	
	    	    	
	    	    	
	    	    	// Receber o certificado do servidor
		    	    byte[] certBytes = (byte[]) in.readObject();
		    	    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
		    	    CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    	    Certificate receivedCert = cf.generateCertificate(bis);

		    	    // Adicionar à keystore local
		    	    userKeystore.setCertificateEntry(userDestinatario, receivedCert);
		    	    try (FileOutputStream fos = new FileOutputStream(userEmissor + ".keystore")) {
		    	        userKeystore.store(fos, passwdkey.toCharArray());
		    	    }
		    	    catch(IOException e) {
		    	    	System.out.println("Password da keystore errada!");
			    		
			    		return;
			    	}
	    	    	
	    	    }
	    	    else {
	    	    	System.out.println(rp567);
	    	    	return;
	    	    }
	    	    
	    	    
	    	    
	    	}
	    	else {
	    		
	    		out.writeObject("já tem");
	    		out.writeObject("já tem certificado");
	    	}
	    	
	    	
	    	
	    	
	        for (String file : ficheirossplit) {
	        	
	        	
	        	
	        	
	            File file2 = new File("C:\\Users\\pedro\\Desktop\\"+file);
	            
	            //File file2 = new File("/home/ALUNOSFC/fc60294/Desktop/"+file);
	            

	            if (!file2.exists()) {
	            	
	                System.out.println("Ficheiro " + file + " não encontrado!");
	                continue;
	            }
				
				
	            
	        	
	            if (comando.equals("-c")) {
	            	
	            	
	            	
	                SecretKey aesKey = utils.generateAESKey();
	                
	                File encryptedFile = new File(file + ".encrypted");
	                utils.encryptFile(file2, encryptedFile, aesKey);
	                sendFile(in,out, encryptedFile);
	                
	                
	                String fileExists = (String) in.readObject();
					
					
		            
		            if(fileExists.equals("já existe ficheiro com esse nome")) {
		            	continue;
		            }
	                
		            
	                utils.guardarKey(out,userDestinatario, aesKey, file, userEmissor,passwdkey);
	                File chavecifrada = new File(file+".secretKey."+userDestinatario);
	                sendFile(in,out,chavecifrada);
	                System.out.println("mandou a chave e o ficheiro cifr");
	                
	                
	                
	                

	            } else if (comando.equals("-s")) {
	            	
	            	// **1. Ler a chave privada da keystore**
	                FileInputStream kfile = new FileInputStream(userEmissor+".keystore");
	                
	                KeyStore kstore = KeyStore.getInstance("PKCS12");
	                try {
	                	kstore.load(kfile, passwdkey.toCharArray());  // password da keystore
	                }
	                catch(IOException e) {
	                	System.out.println("Password da keystore errada!");
			    		
			    		return;
			    	}
	                
	                Key myPrivateKey = kstore.getKey(userEmissor, passwdkey.toCharArray());  // chave privada
	                kfile.close();

	                if (myPrivateKey == null) {
	                    System.out.println("Erro: Não foi possível ler a chave privada!");
	                    return;
	                }
	        
	                
	                byte[] signature = utils.signFile(file2, myPrivateKey);
	                
	                File signature34 = new File(file+".signature."+userEmissor);
	                
	                try (FileOutputStream fos33 = new FileOutputStream(signature34)) {
	                    fos33.write(signature);
	                }
	                
	                
	                File signature35 = new File(file+".signed");
	                Files.copy(file2.toPath(), signature35.toPath(), StandardCopyOption.REPLACE_EXISTING);
	                
	                sendFile(in,out, signature35);
	                
	                String fileExists = (String) in.readObject();
					
					
		            
		            if(fileExists.equals("já existe ficheiro com esse nome")) {
		            	continue;
		            }
	                
		            
	                
	              
	                
	                
	                sendFile(in,out, signature34);
	                
	                

	            } else if (comando.equals("-b")) {
	            	
	            	
	            	
	            	
	            	FileInputStream kfile = new FileInputStream(userEmissor + ".keystore");
	                KeyStore kstore = KeyStore.getInstance("PKCS12");
	                try {
	                	kstore.load(kfile, passwdkey.toCharArray());  // password da keystore
	                }
	                catch(IOException e) {
	                	System.out.println("Password da keystore errada!");
			    		
			    		return;
			    	}
	                
	                
	                Key myPrivateKey = kstore.getKey(userEmissor, passwdkey.toCharArray());  // chave privada
	                kfile.close();

	                if (myPrivateKey == null) {
	                    System.out.println("Erro: Não foi possível ler a chave privada!");
	                    return;
	                }

	                // 1. Copiar file2 para um novo .signed
	                File signature35 = new File(file + ".signed");
	                Files.copy(file2.toPath(), signature35.toPath(), StandardCopyOption.REPLACE_EXISTING);

	                // 2. Gerar chave AES e encriptar ficheiro .signed para .secure
	                SecretKey aesKey = utils.generateAESKey();
	                File encryptedFile = new File(file + ".secure");
	                utils.encryptFile(signature35, encryptedFile, aesKey);

	                // 3. Enviar .secure primeiro
	                sendFile(in, out, encryptedFile);

	                // 4. Verificar se ficheiro já existe
	                String fileExists = (String) in.readObject();
	                if (fileExists.equals("já existe ficheiro com esse nome")) {
	                    continue;
	                }

	               

	                // 5. Assinar ficheiro .signed
	                byte[] signature = utils.signFile(file2, myPrivateKey);
	                File signature34 = new File(file + ".signature." + userEmissor);
	                try (FileOutputStream fos33 = new FileOutputStream(signature34)) {
	                    fos33.write(signature);
	                }

	                // 6. Enviar assinatura (.signature)
	                sendFile(in, out, signature34);

	                // 7. Guardar chave secreta cifrada e enviar
	                utils.guardarKey(out, userDestinatario, aesKey, file, userEmissor,passwdkey);
	                File chavecifrada = new File(file + ".secretKey." + userDestinatario);
	                sendFile(in, out, chavecifrada);
	                
	              
	            }

	            
	        }
	        
	        out.writeObject("Terminou");
	        out.flush();
	        String serverResponse2 = (String) in.readObject();
	        System.out.println(serverResponse2);
	    }

	    out.close();
	    in.close();
	    s.close();
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
	 * Método para receber e processar ficheiros vindos do servidor
	 * @throws Exception 
	 */
	public void receiveFiles(ObjectOutputStream out, ObjectInputStream in, String user, String[] ficheirossplit,String passwdkey) throws Exception {
	    
		
		
		for (String fileName : ficheirossplit) {
			
			
	        out.writeObject(fileName);  

	        String response = (String) in.readObject();
	        System.out.println(response);
	        
	        if (response.equals("ERRO: Ficheiro não encontrado")) {
	            System.out.println("O ficheiro " + fileName + " não existe no servidor.");
	            continue;
	        }
	       
	        String receivedFileName = (String) in.readObject();
	        long fileSize = (long) in.readObject();
	        
	        System.out.println(receivedFileName);
	        System.out.println(fileSize);
	       
	        // Criar diretoria de destino se necessário
	        
	        File pasta = new File("Cliente_"+user);
	        if(!pasta.exists()) {
	        	pasta.mkdirs();
	        }
	        
	        
	        String nomeBase = cortaNomeBase(fileName);
	        
	        if(contemFicheirosComNomeBase(pasta,nomeBase)) {
            	System.out.println("já existe ficheiro com esse nome");
            	out.writeObject("já existe");
            	continue;
            }
	        
	        else {
	        	out.writeObject("n existe");
	        }
	        
	        
	        File receivedFile = new File(pasta , receivedFileName);
	        
	        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
	            byte[] buffer = new byte[2048];
	            int bytesRead;
	            long totalRead = 0;

	            while (totalRead < fileSize) {
	            	
	                bytesRead = in.read(buffer, 0, (fileSize-totalRead)>2048?2048:(int)(fileSize-totalRead));
	                fos.write(buffer, 0, bytesRead);
	                totalRead += bytesRead;
	                
	                
	            }

	            System.out.println("Ficheiro recebido com sucesso! Total: " + totalRead + " bytes");
	        }
	        catch(Exception e ) {
	        	System.out.println("Erro ao receber os ficheiros do servidor.");
	        	
	        }
	        

	        System.out.println("Ficheiro recebidos: " + receivedFileName );

	        // Decifrar se necessário
	        if (receivedFileName.endsWith(".encrypted")) {
	        	
	        	
	        	
	        	String nomedoficheirosolo = fileName.replace(".encrypted",".secretKey."+user);
	        	
		        out.writeObject(nomedoficheirosolo);  

		        String response1 = (String) in.readObject();
		        System.out.println(response1);
		        
		        if (response1.equals("ERRO: Ficheiro não encontrado")) {
		            System.out.println("O ficheiro " + nomedoficheirosolo + " não existe no servidor.");
		            continue;
		        }
		         
		        // Ler nome e tamanho do ficheiro
		        String receivedFileName2 = (String)in.readObject();
		        long fileSize2 = (long) in.readObject();
		        
		        System.out.println(receivedFileName2);
		        System.out.println(fileSize2);
		        
		     
			     
			    File receivedFile2 = new File(pasta,receivedFileName2);
			        
			    	if (!receivedFile.exists()) {
		            	receivedFile.mkdirs();
		                
		            } else {
		                System.out.println("Diretório encontrado com sucesso.");
		            }

			    	
			    
			    System.out.println(receivedFile2.getName());
			    
			    
			    
			    try (FileOutputStream fos2 = new FileOutputStream(receivedFile2)) {
			    	
			            byte[] buffer2 = new byte[2048];
			            int bytesRead2;
			            long totalRead2 = 0;

			            while (totalRead2 < fileSize2) {
			            	
			            	
			            	
			                bytesRead2 = in.read(buffer2, 0, (fileSize2-totalRead2)>2048?2048:(int)(fileSize2-totalRead2));
			                fos2.write(buffer2, 0, bytesRead2);
			                totalRead2 += bytesRead2;
			                
			                
			            }

			            System.out.println("Ficheiro recebido com sucesso! Total: " + totalRead2 + " bytes");
			        }
			        catch(Exception e ) {
			        	System.out.println("Erro ao receber ficheiro do servidor");
			        	
			     }
			     
			     

			    System.out.println("Ficheiro recebidos: " + receivedFileName2 );
			    
			    
			    	
	            Key aesKey = utils.loadAESKey(user,passwdkey);  // Carregar chave do utilizador
	            
	            File ficheirodesencrip = utils.decryptFile(pasta,receivedFile, receivedFile2, aesKey);
	            
	            System.out.println(ficheirodesencrip.getName());
	            
	        }

	        // Verificar assinatura se necessário
	        
	        if (receivedFileName.endsWith(".signed")) {
	        	
	        	
	        	
	        	//receber o nome de quem assinou 
	        	
	        	String nomedoficheirosolo = fileName.replace(".signed", ".signature.");
	        	
		        out.writeObject(nomedoficheirosolo); 
		        
		        String userass = (String) in.readObject();
		        
		        System.out.println(userass);
		       
		        
		        String response1 = (String) in.readObject();
		        
		        System.out.println(response1);
		        
		        if (response1.equals("ERRO: Ficheiro não encontrado")) {
		            System.out.println("O ficheiro " + nomedoficheirosolo + " não existe no servidor.");
		            continue;
		        }
		        
		        // Ler nome e tamanho do ficheiro
		        
		        String receivedFileName2 = (String)in.readObject();
		        
		        System.out.println(receivedFileName2);
		        
		        long fileSize2 = (long) in.readObject();
		        
		        System.out.println(fileSize2);
		        
		        
		        
		     // Criar diretoria de destino se necessário
		        
			     File receivedFile2 = new File(pasta,receivedFileName2);
			     if(!receivedFile.exists()) {
			    	 receivedFile.mkdirs();
			    	 
			     }else {
			    	 System.out.println("Diretório encontrado com sucesso.");
			     }
			     
			     
			     // Guardar ficheiro
			     
			     
			     try (FileOutputStream fos1 = new FileOutputStream(receivedFile2)) {
			    	 byte[] buffer1 = new byte[2048];
			         int bytesRead1;
			         long totalRead1 = 0;

			         while (totalRead1 < fileSize2) {
			        	 bytesRead1 = in.read(buffer1,0,(fileSize2-totalRead1)>2048?2048:(int)(fileSize2-totalRead1));
			        	 fos1.write(buffer1, 0, bytesRead1);
			        	 totalRead1 += bytesRead1;
			         }
			         
			         System.out.println("Ficheiro recebido com sucesso! Total: " + totalRead1 + " bytes");
			         
		            
			     }
			     catch (Exception e) {
			    	 System.out.println("Erro ao receber ficheiro do servidor");
			    	 
			     }
			     
			     
			     
			     System.out.println("Ficheiro recebidos: " + receivedFileName2 );
	            
	        	
	        	byte[] assinatura;
	        	try(FileInputStream sig = new FileInputStream(receivedFileName2)){
	        		assinatura = sig.readAllBytes();
	        	}
	        	
	        	
	        	// **1. Ler o certificado da keystore para obter a chave pública**
	            PublicKey publicKey = utils.loadPublicKey(user,userass,passwdkey);
	            
	            Boolean isValid = utils.verifySignature(receivedFile, assinatura, publicKey);
	            
	            String finali=null;
	            if(isValid) {
	            	finali="válido";
	            }
	            else {
	            	finali="inválido";
	            }
	            
	            System.out.println("Ficheiro: "+receivedFile.getName()+" está : "+ finali);
	             
	        }
	        if (receivedFileName.endsWith(".secure")) {
	        	
	        	String nomedoficheirosolo = fileName.replace(".secure",".secretKey."+user);
		        out.writeObject(nomedoficheirosolo);  
		        
		        
		        
		        
		        
		        String response1 = (String) in.readObject();
		        System.out.println(response1);
		        
		        if (response1.equals("ERRO: Ficheiro não encontrado")) {
		            System.out.println("O ficheiro " + nomedoficheirosolo + " não existe no servidor.");
		            continue;
		        }
		         
		        // Ler nome e tamanho do ficheiro
		        String receivedFileName2 = (String)in.readObject();
		        long fileSize2 = (long) in.readObject();
		        
		        System.out.println(receivedFileName2);
		        System.out.println(fileSize2);
		        
		     
			     
			    File receivedFile2 = new File(pasta,receivedFileName2);
			        
			    	if (!receivedFile.exists()) {
		            	receivedFile.mkdirs();
		                
		            } else {
		                System.out.println("Diretório encontrado com sucesso.");
		            }

			    	
			    
			    System.out.println(receivedFile2.getName());
			        
			    try (FileOutputStream fos2 = new FileOutputStream(receivedFile2)) {
			            byte[] buffer2 = new byte[2048];
			            int bytesRead2;
			            long totalRead2 = 0;

			            while (totalRead2 < fileSize2) {
			            	
			                bytesRead2 = in.read(buffer2, 0, (fileSize2-totalRead2)>2048?2048:(int)(fileSize2-totalRead2));
			                fos2.write(buffer2, 0, bytesRead2);
			                totalRead2 += bytesRead2;
			                
			                
			            }

			            System.out.println("Ficheiro recebido com sucesso! Total: " + totalRead2 + " bytes");
			        }
			        catch(Exception e ) {
			        	System.out.println("Erro ao receber ficheiro do servidor");
			        	
			     }
			        
			         

			    System.out.println("Ficheiro recebidos: " + receivedFileName2 );
			    	
	            Key aesKey = utils.loadAESKey(user,passwdkey);  // Carregar chave do utilizador
	            
	            File ficheirodesencrip = utils.decryptFile(pasta,receivedFile, receivedFile2, aesKey);
	            
	            
	            
	            
	            // parte de verificar a assinatura
	            
	            
	            
	            String nomedoficheirosolo2 = fileName.replace(".secure", ".signature.");
		        out.writeObject(nomedoficheirosolo2);  

		        String useras = (String) in.readObject();
		        
		        
		        String response2 = (String) in.readObject();
		        
		        
		        if (response2.equals("ERRO: Ficheiro não encontrado")) {
		            System.out.println("O ficheiro " + nomedoficheirosolo2 + " não existe no servidor.");
		            continue;
		        }
		        
		        // Ler nome e tamanho do ficheiro
		        String receivedFileName3 = (String)in.readObject();
		        long fileSize3 = (long) in.readObject();
		        
		        
		     // Criar diretoria de destino se necessário
		        
			     File receivedFile3 = new File(pasta,receivedFileName3);
			     if(!receivedFile.exists()) {
			    	 receivedFile.mkdirs();
			    	 
			     }else {
			    	 System.out.println("Diretório encontrado com sucesso.");
			     }
			     
			     
			     // Guardar ficheiro
			     
			     
			     try (FileOutputStream fos1 = new FileOutputStream(receivedFile3)) {
			    	 byte[] buffer1 = new byte[2048];
			         int bytesRead1;
			         long totalRead1 = 0;

			         while (totalRead1 < fileSize3) {
			        	 bytesRead1 = in.read(buffer1,0,(fileSize3-totalRead1)>2048?2048:(int)(fileSize3-totalRead1));
			        	 fos1.write(buffer1, 0, bytesRead1);
			        	 totalRead1 += bytesRead1;
			         }
			         
			         System.out.println("Ficheiro recebido com sucesso! Total: " + totalRead1 + " bytes");
			         
		            
			     }
			     catch (Exception e) {
			    	 System.out.println("Erro ao receber ficheiro do servidor");
			    	 
			     }
			     
			     System.out.println("Ficheiro recebidos: " + receivedFileName3 );
	            
	        	
	        	byte[] assinatura;
	        	try(FileInputStream sig = new FileInputStream(receivedFileName3)){
	        		assinatura = sig.readAllBytes();
	        	}
	        	
	        	
	        	// **1. Ler o certificado da keystore para obter a chave pública**
	            PublicKey publicKey = utils.loadPublicKey(user,useras,passwdkey);
	            
	            Boolean isValid = utils.verifySignature(ficheirodesencrip, assinatura, publicKey);
	            
	            String finali=null;
	            if(isValid) {
	            	finali="válido";
	            }
	            else {
	            	finali="inválido";
	            }
	            
	            System.out.println("Ficheiro: "+ficheirodesencrip.getName()+" está : "+ finali);
	            
	            
	        	
	        }
	        
	    }	
	    out.writeObject("Terminou");
	}

	
	
	private static void sendFile(ObjectInputStream in,ObjectOutputStream out, File file) throws IOException, ClassNotFoundException {
		
        out.writeObject(file.getName());
        out.writeObject(file.length());
        
        String fileExists = (String) in.readObject();
        System.out.println("Resposta do servidor: " + fileExists);
        Boolean flag = true;
        while(flag) {
        	
        	if (fileExists.equals("já existe ficheiro com esse nome")) {
	            break;
	        }
	        
	        try (FileInputStream fis = new FileInputStream(file)) {
	            byte[] buffer = new byte[4096];
	            int bytesRead;
	            while ((bytesRead = fis.read(buffer)) != -1) {
	                out.write(buffer, 0, bytesRead);
	            }
	        }
	        out.flush();
	        flag=false;
        	
        }
	        
	
    }
}

