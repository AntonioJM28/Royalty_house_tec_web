package com.royaltyhouse.dto.form;

import com.royaltyhouse.model.TipoRecurso;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservaBillarForm {

    @NotNull
    private Long recursoId;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer bloques;

    private TipoRecurso tipo = TipoRecurso.MESA_NORMAL;

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Integer getBloques() {
        return bloques;
    }

    public void setBloques(Integer bloques) {
        this.bloques = bloques;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public void setTipo(TipoRecurso tipo) {
        this.tipo = tipo;
    }
}
