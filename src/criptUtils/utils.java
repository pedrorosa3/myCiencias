package criptUtils;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class utils {

    private static final String AES_ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 128;
    

    // Gera uma chave AES aleatória
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    // Cifra um ficheiro usando AES
    public static void encryptFile(File inputFile, File outputFile, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        processFile(cipher, inputFile, outputFile);
    }

    // Decifra um ficheiro usando AES
    public static File decryptFile(File pasta,File fileencriptado, File chave, Key secretKey) throws Exception {
    	
    	FileInputStream keyFile = new FileInputStream(chave);
        byte[] encryptedSecretKey = keyFile.readAllBytes();
        keyFile.close();
        
        
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] secretKeyEncoded = rsaCipher.doFinal(encryptedSecretKey);
    	
        SecretKeySpec secretKey1 = new SecretKeySpec(secretKeyEncoded, "AES");
        
        
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey1);
        
        String nomeficheirodecript;
        
        if(fileencriptado.getName().contains(".secure")) {
        	
        	nomeficheirodecript = fileencriptado.getName().replace(".secure", "");
        }
        else {
        	nomeficheirodecript = fileencriptado.getName().replace(".encrypted", "");
        }
        
        
        //nomeficheirodecript.replace(".secure", "");
        File ficheirodecript = new File(pasta,nomeficheirodecript);
        
        
        
        
        try (FileInputStream fis = new FileInputStream(fileencriptado);
             CipherInputStream cis = new CipherInputStream(fis, aesCipher);
             FileOutputStream fos = new FileOutputStream(ficheirodecript)) {

            byte[] buffer = new byte[16];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        System.out.println("Ficheiro decifrado com sucesso: "+fileencriptado.getName() + " para o ficheiro: "+ ficheirodecript.getName());
        
        return ficheirodecript;
         
    }

    // Assina um ficheiro
    public static byte[] signFile(File file, Key myPrivateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign((PrivateKey) myPrivateKey);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }
        }
        return signature.sign();
    }

    // Verifica a assinatura de um ficheiro
    public static boolean verifySignature(File file, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }
        }
        return signature.verify(signatureBytes);
    }

    // Método auxiliar para processar ficheiros durante a criptografia/decifragem
    private static void processFile(Cipher cipher, File inputFile, File outputFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            cos.close();
        }
        
        
    }

	public static PublicKey loadPublicKey(String userDestinatario, String userassinate,String passwdkey) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileInputStream kfile2 = new FileInputStream(userDestinatario+".keystore");
		KeyStore kstore = KeyStore.getInstance("PKCS12");
		kstore.load(kfile2, passwdkey.toCharArray()); 
		Certificate cert = kstore.getCertificate(userassinate);  // obter o certificado
        kfile2.close();
        if (cert == null) {
            throw new KeyStoreException("Certificado de " + userassinate + " não encontrado na keystore de " + userDestinatario);
        }
        PublicKey publicKey = cert.getPublicKey();   // chave pública
        if (publicKey == null) {
            System.out.println("Erro: Não foi possível ler a chave pública!");
            return null;
        }
		
		return publicKey;
		
	}
	
	public static void guardarKey (ObjectOutputStream out,String user, SecretKey aesKey, String file, String userEmissor,String passwdkey) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		
		
		// Usa a keystore do EMISSOR, onde está guardado o certificado do destinatário
		KeyStore kstore = KeyStore.getInstance("PKCS12");
		try (FileInputStream kfile = new FileInputStream(userEmissor+".keystore")) {
        	kstore.load(kfile, passwdkey.toCharArray());  // password da keystore
        }

        catch(IOException e) {
        	System.out.println("Palavra passe da key store incorreta!");
    		return;
    	}

        Certificate cert = kstore.getCertificate(user);
        if (cert == null) {
        	System.out.println("Erro: certificado de " + user + " não encontrado na keystore de " + userEmissor);
        	return;
        }
        PublicKey publicKey = cert.getPublicKey();

        // **Cifrar a chave secreta AES com a chave pública RSA**
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] secretKeyEncoded = aesKey.getEncoded();
        byte[] encryptedSecretKey = rsaCipher.doFinal(secretKeyEncoded);

        // **Guardar a chave secreta cifrada no ficheiro a.key**
        try (FileOutputStream kos = new FileOutputStream(file + ".secretKey." + user)) {
            kos.write(encryptedSecretKey);
            
        }
        

        
    }

	public static Key loadAESKey(String user,String passwdkey) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		FileInputStream kfile2 = new FileInputStream(user+".keystore");
        KeyStore kstore = KeyStore.getInstance("PKCS12");
        kstore.load(kfile2, passwdkey.toCharArray());  // password da keystore
        Key myPrivateKey = kstore.getKey(user, passwdkey.toCharArray());  // chave privada
        kfile2.close();
		return  myPrivateKey;
	}
}

