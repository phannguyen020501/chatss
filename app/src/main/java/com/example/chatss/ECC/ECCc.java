package com.example.chatss.ECC;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class ECCc {
    public static byte[] iv = new SecureRandom().generateSeed(16);

    private static final String KEYSTORE_PATH = "app/keystore";
    private static final String KEYSTORE_PASSWORD = "123456";


    private static final String KEYSTORE_FILENAME = "keystore.bks";

    public static void main(String[] args) throws Exception {

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());

        String plainText = "Look mah, I'm a message!";
        System.out.println("Original plaintext message: " + plainText);

        // Initialize two key pairs
        KeyPair keyPairA = generateECKeys();
        KeyPair keyPairB = generateECKeys();
//        System.out.println("public key a: " + Hex.toHexString(keyPairA.getPublic().getEncoded()));
        // Create two AES secret keys to encrypt/decrypt the message


//
//        PublicKey publicKey1 = keyPairA.getPublic();
//        PublicKey publicKey2 = keyPairB.getPublic();
//        PrivateKey privateKey1 = keyPairA.getPrivate();
//        PrivateKey privateKey2 = keyPairB.getPrivate();
//
//        System.out.println("to string");
//        System.out.println("public key");
//        String pubString1 = publicKeyToString(publicKey1);
//        String pubString2 = publicKeyToString(publicKey2);
//        System.out.println(pubString1);
//        System.out.println(pubString2);
//
//        System.out.println("private key");
//        String priString1 = privateKeyToString(privateKey1);
//        String priString2 = privateKeyToString(privateKey2);
//        System.out.println(priString1);
//        System.out.println(priString2);
//
//        System.out.println("string to key");
//        PublicKey pub1 = stringToPublicKey(pubString1);
//        PublicKey pub2 = stringToPublicKey(pubString2);
//        System.out.println(pub1);
//        System.out.println(pub2);
//
//        PrivateKey pri1 = stringToPrivateKey(priString1);
//        PrivateKey pri2 = stringToPrivateKey(priString2);
//        System.out.println(pri1);
//        System.out.println(pri2);
//
//        SecretKey secretKeyA = generateSharedSecret(pri1, pub2);
//        SecretKey secretKeyB = generateSharedSecret(pri2, pub1);
//
//        // Encrypt the message using 'secretKeyA'
//        String cipherText = encryptString(secretKeyA, plainText);
//        System.out.println("Encrypted cipher text: " + cipherText);
//
//        // Decrypt the message using 'secretKeyB'
//        String decryptedPlainText = decryptString(secretKeyB, cipherText);
//        System.out.println("Decrypted cipher text: " + decryptedPlainText);

//          createKeyStore();


//        KeyStore keyStore = loadKeyStore();
//        if(keyStore != null){
//            System.out.println("đã có keystore");
//        }else{
//            System.out.println("ko có keystore");
//        }

        String alias = "b3@gmail.com";
        String password = "123456";
        KeyPair k = generateECKeys();
        //savePrivateKey(alias, password, k );
        //PrivateKey p3 = getPrivateKeyFromKeyStore(alias, password);
        //System.out.println(privateKeyToString(p3));
        //keyStore = loadKeyStore();
        //System.out.println("Loaded keystore: " + keyStore.size() + " entries");

    }
    public static void savePrivateKey(Context context, String email, String password, KeyPair keyPair) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        // Tạo một khóa bí mật mới
//        KeyPair keyPair =generateECKeys();
        System.out.println("keypair");
        System.out.println(keyPair.getPrivate());
        System.out.println(keyPair.getPublic());


        // Tạo một certificate tự ký
        X509Certificate selfSignedCert = generateSelfSignedCertificate(keyPair);
        System.out.println(selfSignedCert);


        // Lưu khóa bí mật và chứng chỉ vào keystore
        KeyStore keyStore = loadKeyStore(context);
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[]{selfSignedCert});
        KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(password.toCharArray());
        keyStore.setEntry(email, privateKeyEntry, passwordProtection);

        FileOutputStream fos = new FileOutputStream(getKeyStoreFile(context));

        //FileOutputStream fos = new FileOutputStream(KEYSTORE_PATH);
        keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
        fos.close();

    }

    public static PrivateKey getPrivateKeyFromKeyStore(Context context, String alias, String keyPassword) {
        try {
            // Load the KeyStore
            KeyStore keyStore =loadKeyStore(context);

            // Get the private key using the alias and key password
            Key key = keyStore.getKey(alias, keyPassword.toCharArray());
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            } else {
                System.err.println("The specified alias does not contain a private key.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public static KeyPair generateECKeys1() {
        try {
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    "EC", "BC");

            keyPairGenerator.initialize(parameterSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            return keyPair;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                 | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static KeyPair generateECKeys() {
        try {
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(parameterSpec); // Change the key size as per your requirement
            return keyPairGenerator.generateKeyPair();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SecretKey generateSharedSecret(PrivateKey privateKey,
                                                 PublicKey publicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);

            return keyAgreement.generateSecret("AES");
        } catch (InvalidKeyException | NoSuchAlgorithmException
                 | NoSuchProviderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    public static String encryptString(SecretKey secretKey, String data) {
        String encryptedText = "";

        if (data == null || secretKey == null)
            return encryptedText;

        try {
            Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, new SecureRandom());//new IvParameterSpec(getIV()) - if you want custom IV

            //encrypted data:
            byte[] encryptedBytes = encryptCipher.doFinal(data.getBytes("UTF-8"));

            //take IV from this cipher
            byte[] iv = encryptCipher.getIV();

            //append Initiation Vector as a prefix to use it during decryption:
            byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];

            //populate payload with prefix IV and encrypted data
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length);

            encryptedText = Base64.encodeToString(combinedPayload, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException | IllegalBlockSizeException | UnsupportedEncodingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return encryptedText;
    }

    public static String decryptString(SecretKey secretKey, String encryptedString) {
        String decryptedText = "";

        if (encryptedString == null || secretKey == null)
            return decryptedText;

        try {
            //separate prefix with IV from the rest of encrypted data
            byte[] encryptedPayload = Base64.decode(encryptedString, Base64.DEFAULT);
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[encryptedPayload.length - iv.length];

            //populate iv with bytes:
            System.arraycopy(encryptedPayload, 0, iv, 0, 16);

            //populate encryptedBytes with bytes:
            System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length);

            Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
            decryptedText = new String(decryptedBytes);

        } catch (NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return decryptedText;
    }
//    public static String encryptString(SecretKey key, String plainText) {
//        try {
//            IvParameterSpec ivSpec = new IvParameterSpec(iv);
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
//            byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
//            byte[] cipherText;
//
//            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
//            cipherText = new byte[cipher.getOutputSize(plainTextBytes.length)];
//            int encryptLength = cipher.update(plainTextBytes, 0,
//                    plainTextBytes.length, cipherText, 0);
//            encryptLength += cipher.doFinal(cipherText, encryptLength);
//
//            return bytesToHex(cipherText);
//        } catch (NoSuchAlgorithmException | NoSuchProviderException
//                 | NoSuchPaddingException | InvalidKeyException
//                 | InvalidAlgorithmParameterException
//                 | ShortBufferException
//                 | IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    public static String decryptString(SecretKey key, String cipherText) {
//        try {
//            Key decryptionKey = new SecretKeySpec(key.getEncoded(),
//                    key.getAlgorithm());
//            IvParameterSpec ivSpec = new IvParameterSpec(iv);
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
//            byte[] cipherTextBytes = hexToBytes(cipherText);
//            byte[] plainText;
//
//            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec);
//            plainText = new byte[cipher.getOutputSize(cipherTextBytes.length)];
//            int decryptLength = cipher.update(cipherTextBytes, 0,
//                    cipherTextBytes.length, plainText, 0);
//            decryptLength += cipher.doFinal(plainText, decryptLength);
//
//            return new String(plainText, "UTF-8");
//        } catch (NoSuchAlgorithmException | NoSuchProviderException
//                 | NoSuchPaddingException | InvalidKeyException
//                 | InvalidAlgorithmParameterException
//                 | IllegalBlockSizeException | BadPaddingException
//                 | ShortBufferException | UnsupportedEncodingException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static String bytesToHex(byte[] data, int length) {
        String digits = "0123456789ABCDEF";
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i != length; i++) {
            int v = data[i] & 0xff;

            buffer.append(digits.charAt(v >> 4));
            buffer.append(digits.charAt(v & 0xf));
        }

        return buffer.toString();
    }

    public static String bytesToHex(byte[] data) {
        return bytesToHex(data, data.length);
    }

    public static byte[] hexToBytes(String string) {
        int length = string.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character
                    .digit(string.charAt(i + 1), 16));
        }
        return data;
    }


    public static String publicKeyToString(PublicKey publicKey) throws IOException {
//        Security.addProvider(new BouncyCastleProvider());

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        PemObject pemObject = new PemObject("PUBLIC KEY", subjectPublicKeyInfo.getEncoded());

        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(pemObject);
        pemWriter.close();

        return stringWriter.toString();
    }

    public static PublicKey stringToPublicKey(String publicKeyString) throws Exception {
//        Security.addProvider(new BouncyCastleProvider());

        // Đọc chuỗi PEM
        PEMParser pemParser = new PEMParser(new StringReader(publicKeyString));
        SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();

        // Chuyển đổi SubjectPublicKeyInfo sang X509EncodedKeySpec
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded());

        // Tạo khóa công khai từ X509EncodedKeySpec
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        return publicKey;
    }

    public static PrivateKey stringToPrivateKey(String privateKeyString) throws Exception {
//        Security.addProvider(new BouncyCastleProvider());

        // Đọc chuỗi PEM
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyString));
        Object object = pemParser.readObject();
        pemParser.close();

        // Chuyển đổi PrivateKeyInfo sang PKCS8EncodedKeySpec
        PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());

        // Tạo khóa bí mật từ PKCS8EncodedKeySpec
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return privateKey;
    }

    public static String privateKeyToString(PrivateKey privateKey) throws IOException {
//        Security.addProvider(new BouncyCastleProvider());
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
        PemObject pemObject = new PemObject("PRIVATE KEY", privateKeyInfo.getEncoded());

        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(pemObject);
        pemWriter.close();

        return stringWriter.toString();
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws CertificateException {
        try {
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name("CN=Test"),
                    new BigInteger(64, new SecureRandom()),
                    new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24),
                    new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365)),
                    new X500Name("CN=Test"),
                    keyPair.getPublic()
            );

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithECDSA")
                    .build(keyPair.getPrivate());

            return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
        } catch (Exception e) {
            throw new CertificateException("Failed to generate self-signed certificate", e);
        }
    }

    public static X509Certificate generateCertificate(KeyPair keyPair) throws CertificateEncodingException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        Date startDate = new Date(); // Ngày bắt đầu chứng chỉ
        Date expiryDate = new Date(startDate.getTime() + 365 * 24 * 60 * 60 * 1000L); // Ngày hết hạn chứng chỉ
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number
        cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc
        cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed
        cert.setPublicKey(keyPair.getPublic());
        cert.setNotBefore(startDate);
        cert.setNotAfter(expiryDate);
        cert.setSignatureAlgorithm("SHA256withECDSA");
        PrivateKey signingKey = keyPair.getPrivate();
        return cert.generate(signingKey);
    }

    public static KeyStore loadKeyStore(Context context) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        KeyStore keyStore = KeyStore.getInstance("BKS");

        //FileInputStream fis = new FileInputStream(KEYSTORE_PATH);

        File keyStoreFile = getKeyStoreFile(context);
        if (!keyStoreFile.exists()) {
            keyStore.load(null, KEYSTORE_PASSWORD.toCharArray());
            FileOutputStream fos = new FileOutputStream(keyStoreFile);
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            fos.close();
        } else {
            FileInputStream fis = new FileInputStream(keyStoreFile);
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            fis.close();
        }
        return keyStore;
    }

    private static File getKeyStoreFile(Context context) {
        return new File(context.getFilesDir(), KEYSTORE_FILENAME);
    }


    public static KeyStore createKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        KeyStore keyStore = KeyStore.getInstance("BKS");
        keyStore.load(null, KEYSTORE_PASSWORD.toCharArray());

        FileOutputStream fos = new FileOutputStream(KEYSTORE_PATH);
        keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
        fos.close();
        return keyStore;
    }

}