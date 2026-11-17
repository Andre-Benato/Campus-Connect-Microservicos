package com.campusconnect.user.application;

import com.campusconnect.common.EmailAlreadyUsedException;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserCreateRequest;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserResponse;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserUpdateRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.user.domain.User;
import com.campusconnect.user.infrastructure.adapters.out.persistence.UserRepository;
/** Regras de negocio do usuario (RF01). */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse criar(UserCreateRequest request) {
        String email = normalizarEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        User user =
                new User(
                        request.name().trim(), email, passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listar() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse buscarPorId(Long id) {
        return UserResponse.from(buscarEntidade(id));
    }

    /**
     * Atualiza o cadastro. Um usuario so pode alterar o proprio registro, por isso o id autenticado
     * e conferido antes de qualquer consulta: responder 404 para o id de outra pessoa revelaria
     * quais ids existem.
     */
    @Transactional
    public UserResponse atualizar(Long id, UserUpdateRequest request, Long usuarioAutenticadoId) {
        garantirQueEhOProprioUsuario(id, usuarioAutenticadoId);

        User user = buscarEntidade(id);

        String email = normalizarEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyUsedException(email);
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    /** Remove o cadastro. Assim como a atualizacao, so vale para o proprio registro. */
    @Transactional
    public void remover(Long id, Long usuarioAutenticadoId) {
        garantirQueEhOProprioUsuario(id, usuarioAutenticadoId);

        User user = buscarEntidade(id);
        userRepository.delete(user);
    }

    private void garantirQueEhOProprioUsuario(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new ForbiddenOperationException(
                    "Um usuario so pode alterar ou remover o proprio cadastro");
        }
    }

    private User buscarEntidade(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Usuario com id " + id + " nao encontrado"));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
