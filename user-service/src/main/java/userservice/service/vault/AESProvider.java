package userservice.service.vault;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESProvider {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_SIZE = 16;

    private final SecretKeySpec secretKey;

    public AESProvider(String key) {
        // key를 32바이트(256bit)로 맞춤
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] normalizedKey = new byte[32];
        System.arraycopy(keyBytes, 0, normalizedKey, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(normalizedKey, KEY_ALGORITHM);
    }

    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // IV 생성
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // IV + 암호화된 데이터를 합쳐서 Base64 인코딩
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String cipherText) throws Exception {
        byte[] combined = Base64.getDecoder().decode(cipherText);

        // IV 추출
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(combined, 0, iv, 0, IV_SIZE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // 암호화된 데이터 추출
        byte[] encrypted = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
