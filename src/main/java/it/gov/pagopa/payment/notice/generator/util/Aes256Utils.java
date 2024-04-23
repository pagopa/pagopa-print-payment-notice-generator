package it.gov.pagopa.payment.notice.generator.util;

import it.gov.pagopa.payment.notice.generator.exception.Aes256Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class Aes256Utils {

    private String aesSecretKey;

    private String aesSalt;

    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    public static final String PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    public static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";

    public static final String ALGORITHM = "AES";

    private static final int AES_UNEXPECTED_ERROR = 701;

    @Autowired
    public Aes256Utils(
            @Value("${aes.secret.key}") String aesSecretKey,
            @Value("${aes.salt}") String aesSalt) {
        this.aesSecretKey = aesSecretKey;
        this.aesSalt = aesSalt;
    }

    public String encrypt(String strToEncrypt) throws Aes256Exception {

        try {

            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
            KeySpec spec = new PBEKeySpec(aesSecretKey.toCharArray(), aesSalt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

            //Padding vulnerability rule java:S5542 ignored because encryption is used inside application workflow
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new Aes256Exception("Unexpected error when encrypting the given string", AES_UNEXPECTED_ERROR, e);
        }
    }

    public String decrypt(String strToDecrypt) throws Aes256Exception {
        try{
            byte[] encryptedData = Base64.getDecoder().decode(strToDecrypt);
            byte[] iv = new byte[16];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
            KeySpec spec = new PBEKeySpec(aesSecretKey.toCharArray(), aesSalt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

            //Padding vulnerability rule java:S5542 ignored because decryption is used inside application workflow
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);

            byte[] cipherText = new byte[encryptedData.length - 16];
            System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);

            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Aes256Exception("Unexpected error when decrypting the given string", AES_UNEXPECTED_ERROR, e);
        }
    }
}

