package com.campusconnect.apartment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentCreateRequest;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentResponse;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentUpdateRequest;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.campusconnect.apartment.domain.Apartment;
import com.campusconnect.apartment.infrastructure.adapters.out.persistence.ApartmentRepository;
@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {

    @Mock
    private ApartmentRepository apartmentRepository;

    private ApartmentService apartmentService;

    @BeforeEach
    void setUp() {
        apartmentService = new ApartmentService(apartmentRepository);
    }

    @Test
    void deveCriarApartamentoNormalizandoEstadoEAssociandoDono() {
        ApartmentCreateRequest request =
                new ApartmentCreateRequest(
                        " Kitnet Campus ",
                        "Perto da faculdade",
                        " Rua das Flores ",
                        " 100 ",
                        null,
                        " Centro ",
                        " Curitiba ",
                        " pr ",
                        "80000-000",
                        1,
                        1,
                        new BigDecimal("1200.00"),
                        new BigDecimal("150.00"));

        when(apartmentRepository.save(any(Apartment.class)))
                .thenAnswer(
                        invocation -> {
                            Apartment saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", 10L);
                            return saved;
                        });

        ApartmentResponse response = apartmentService.criar(request, 7L);

        ArgumentCaptor<Apartment> captor = ArgumentCaptor.forClass(Apartment.class);
        verify(apartmentRepository).save(captor.capture());
        Apartment persisted = captor.getValue();

        assertThat(persisted.getName()).isEqualTo("Kitnet Campus");
        assertThat(persisted.getState()).isEqualTo("PR");
        assertThat(persisted.getUserId()).isEqualTo(7L);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.available()).isTrue();
    }

    @Test
    void deveLancarNotFoundQuandoApartamentoNaoExiste() {
        when(apartmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apartmentService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveRecusarAtualizacaoQuandoUsuarioNaoEDono() {
        Apartment apartment = apartamentoExistente(5L, 1L);
        when(apartmentRepository.findById(5L)).thenReturn(Optional.of(apartment));

        ApartmentUpdateRequest request =
                new ApartmentUpdateRequest(
                        "Nome",
                        null,
                        "Rua",
                        "1",
                        null,
                        "Bairro",
                        "Cidade",
                        "SP",
                        "01000-000",
                        2,
                        1,
                        new BigDecimal("900.00"),
                        null,
                        false);

        assertThatThrownBy(() -> apartmentService.atualizar(5L, request, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(apartmentRepository, never()).save(any());
    }

    @Test
    void deveRemoverQuandoUsuarioEDono() {
        Apartment apartment = apartamentoExistente(5L, 1L);
        when(apartmentRepository.findById(5L)).thenReturn(Optional.of(apartment));

        apartmentService.remover(5L, 1L);

        verify(apartmentRepository).delete(apartment);
    }

    private static Apartment apartamentoExistente(Long id, Long userId) {
        Apartment apartment =
                new Apartment(
                        "Kitnet",
                        null,
                        "Rua A",
                        "10",
                        null,
                        "Centro",
                        "Curitiba",
                        "PR",
                        "80000-000",
                        1,
                        1,
                        new BigDecimal("1000.00"),
                        null,
                        userId);
        ReflectionTestUtils.setField(apartment, "id", id);
        return apartment;
    }
}
