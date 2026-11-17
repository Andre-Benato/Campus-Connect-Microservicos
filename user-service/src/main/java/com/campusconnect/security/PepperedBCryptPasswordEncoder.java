package com.campusconnect.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt com um secret de aplicacao (pepper).
 *
 * <p>A senha passa primeiro por um HMAC-SHA256 com o secret configurado e o resultado em Base64 e
 * entao processado pelo BCrypt. Isso mantem o salt por senha do BCrypt e adiciona um segredo que nao
 * fica armazenado no banco: um vazamento apenas da tabela {@code users} nao permite testar senhas
 * por forca bruta sem tambem obter o secret.
 *
 * <p>O HMAC tambem normaliza o tamanho da entrada (44 caracteres), o que evita o limite de 72 bytes
 * do BCrypt.
 *
 * <p><strong>Atencao:</strong> alterar o secret invalida todos os hashes ja gravados.
 */
public class PepperedBCryptPasswordEncoder implements PasswordEncoder {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final BCryptPasswordEncoder bcrypt;
    private final SecretKeySpec key;

    public PepperedBCryptPasswordEncoder(String secret, int strength) {
        this.bcrypt = new BCryptPasswordEncoder(strength);
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(pepper(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return bcrypt.matches(pepper(rawPassword), encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return bcrypt.upgradeEncoding(encodedPassword);
    }

    private String pepper(CharSequence rawPassword) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Nao foi possivel aplicar o secret na senha", e);
        }
    }
}
