package com.campusconnect.auth.application;

import com.campusconnect.auth.infrastructure.adapters.in.web.dto.LoginRequest;
import com.campusconnect.auth.infrastructure.adapters.in.web.dto.LoginResponse;
import com.campusconnect.common.InvalidCredentialsException;
import com.campusconnect.user.domain.User;
import com.campusconnect.user.infrastructure.adapters.out.persistence.UserRepository;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserResponse;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negocio do login e do logout (RF02). */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Hash descartavel usado quando o e-mail nao existe.
     *
     * <p>Sem ele, uma tentativa com e-mail inexistente responderia bem mais rapido do que uma com
     * senha errada (que precisa rodar o BCrypt), e essa diferenca de tempo revelaria quais e-mails
     * estao cadastrados.
     */
    private final String hashDescartavel;

    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashDescartavel = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<User> encontrado = userRepository.findByEmail(email);

        if (encontrado.isEmpty()) {
            passwordEncoder.matches(request.password(), hashDescartavel);
            throw new InvalidCredentialsException();
        }

        User user = encontrado.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        JwtService.TokenEmitido token = jwtService.emitir(user);
        log.info("Login realizado pelo usuario {}", user.getId());

        return new LoginResponse(
                token.valor(),
                LoginResponse.BEARER,
                token.validade().toSeconds(),
                token.expiraEm(),
                UserResponse.from(user));
    }

    /**
     * Invalida o token usado na requisicao. Chamar o logout duas vezes com o mesmo token nao e erro:
     * o resultado desejado (token invalido) ja foi alcancado.
     */
    public void logout(Jwt token) {
        try {
            jwtService.revogar(token);
        } catch (DataIntegrityViolationException e) {
            // Dois logouts simultaneos com o mesmo token: o indice unico barrou o segundo registro,
            // mas o token ja esta revogado pelo primeiro.
            log.debug("Token {} ja havia sido revogado", token.getId());
        }
        log.info("Logout realizado pelo usuario {}", token.getSubject());
    }
}
