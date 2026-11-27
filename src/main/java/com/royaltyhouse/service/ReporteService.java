package com.royaltyhouse.service;

import com.royaltyhouse.model.ReporteServicio;
import com.royaltyhouse.repository.ReporteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteService {
    private final ReporteRepository repository;

    public ReporteService(ReporteRepository repository) { this.repository = repository; }

    public List<ReporteServicio> generar(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new com.royaltyhouse.exception.ReglaNegocioException("El rango de fechas no es válido.");
        }
        return repository.generar(desde,hasta);
    }
}
