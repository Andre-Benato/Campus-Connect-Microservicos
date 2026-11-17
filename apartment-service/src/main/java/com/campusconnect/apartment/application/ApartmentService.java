package com.campusconnect.apartment.application;

import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentCreateRequest;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentFilter;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentResponse;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentUpdateRequest;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.apartment.domain.Apartment;
import com.campusconnect.apartment.infrastructure.adapters.out.persistence.ApartmentRepository;
import com.campusconnect.apartment.infrastructure.adapters.out.persistence.ApartmentSpecifications;
@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @Transactional
    public ApartmentResponse criar(ApartmentCreateRequest request, Long usuarioId) {
        Apartment apartment =
                new Apartment(
                        request.name().trim(),
                        request.description(),
                        request.address().trim(),
                        request.number().trim(),
                        request.complement(),
                        request.neighborhood().trim(),
                        request.city().trim(),
                        request.state().trim().toUpperCase(),
                        request.cep().trim(),
                        request.bedrooms(),
                        request.bathrooms(),
                        request.rentPrice(),
                        request.condominiumPrice(),
                        usuarioId);

        return ApartmentResponse.from(apartmentRepository.save(apartment));
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> listar(ApartmentFilter filter) {
        return apartmentRepository.findAll(ApartmentSpecifications.from(filter)).stream()
                .map(ApartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> listarDoUsuario(Long usuarioId) {
        return apartmentRepository.findByUserId(usuarioId).stream()
                .map(ApartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApartmentResponse buscarPorId(Long id) {
        return ApartmentResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ApartmentResponse atualizar(
            Long id, ApartmentUpdateRequest request, Long usuarioId) {

        Apartment apartment = buscarEntidade(id);
        garantirDono(apartment, usuarioId);

        apartment.setName(request.name().trim());
        apartment.setDescription(request.description());
        apartment.setAddress(request.address().trim());
        apartment.setNumber(request.number().trim());
        apartment.setComplement(request.complement());
        apartment.setNeighborhood(request.neighborhood().trim());
        apartment.setCity(request.city().trim());
        apartment.setState(request.state().trim().toUpperCase());
        apartment.setCep(request.cep().trim());
        apartment.setBedrooms(request.bedrooms());
        apartment.setBathrooms(request.bathrooms());
        apartment.setRentPrice(request.rentPrice());
        apartment.setCondominiumPrice(request.condominiumPrice());
        apartment.setAvailable(request.available());

        return ApartmentResponse.from(apartmentRepository.save(apartment));
    }

    @Transactional
    public void remover(Long id, Long usuarioId) {
        Apartment apartment = buscarEntidade(id);
        garantirDono(apartment, usuarioId);
        apartmentRepository.delete(apartment);
    }

    private Apartment buscarEntidade(Long id) {
        return apartmentRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Apartamento com id " + id + " nao encontrado"));
    }

    private void garantirDono(Apartment apartment, Long usuarioId) {
        if (!apartment.getUserId().equals(usuarioId)) {
            throw new ForbiddenOperationException(
                    "Um usuario so pode alterar ou remover seus proprios apartamentos");
        }
    }
}
