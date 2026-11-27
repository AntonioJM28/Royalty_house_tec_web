package com.royaltyhouse.repository;

import com.royaltyhouse.model.ReporteServicio;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReporteRepository {
    private final JdbcClient jdbc;

    public ReporteRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<ReporteServicio> generar(LocalDate desde, LocalDate hasta) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias < 1) dias = 1;

        Integer billarRecursos = jdbc.sql("SELECT COUNT(*) FROM recurso WHERE activo=TRUE AND tipo IN ('MESA_NORMAL','MESA_VIP')")
                .query(Integer.class).single();
        Integer minicineRecursos = jdbc.sql("SELECT COUNT(*) FROM recurso WHERE activo=TRUE AND tipo='SALA_MINICINE'")
                .query(Integer.class).single();
        var horario = jdbc.sql("SELECT hora_apertura,hora_cierre FROM horario_atencion ORDER BY id LIMIT 1")
                .query((rs, n) -> new Object[]{rs.getTime("hora_apertura").toLocalTime(), rs.getTime("hora_cierre").toLocalTime()}).optional().orElse(new Object[]{java.time.LocalTime.of(10,0), java.time.LocalTime.of(23,0)});
        int horasAtencion = ((java.time.LocalTime) horario[1]).getHour() - ((java.time.LocalTime) horario[0]).getHour();

        Integer horasBillar = jdbc.sql("""
                SELECT COALESCE(SUM(r.bloques),0) FROM reserva r
                JOIN recurso rec ON rec.id=r.recurso_id
                WHERE r.estado='FINALIZADA' AND r.fecha BETWEEN :desde AND :hasta
                AND rec.tipo IN ('MESA_NORMAL','MESA_VIP')
                """).param("desde",desde).param("hasta",hasta).query(Integer.class).single();
        BigDecimal ingresosBillar = jdbc.sql("""
                SELECT COALESCE(SUM(r.bloques*t.precio_hora),0) FROM reserva r
                JOIN recurso rec ON rec.id=r.recurso_id
                JOIN tarifa t ON t.tipo_recurso=rec.tipo
                WHERE r.estado='FINALIZADA' AND r.fecha BETWEEN :desde AND :hasta
                AND rec.tipo IN ('MESA_NORMAL','MESA_VIP')
                """).param("desde",desde).param("hasta",hasta).query(BigDecimal.class).single();

        Integer bloquesMinicine = jdbc.sql("""
                SELECT COALESCE(SUM(r.bloques),0) FROM reserva r
                JOIN recurso rec ON rec.id=r.recurso_id
                WHERE r.estado='FINALIZADA' AND r.fecha BETWEEN :desde AND :hasta AND rec.tipo='SALA_MINICINE'
                """).param("desde",desde).param("hasta",hasta).query(Integer.class).single();
        BigDecimal tarifaPersona = jdbc.sql("SELECT COALESCE(precio_hora,0) FROM tarifa WHERE tipo_recurso='SALA_MINICINE'")
                .query(BigDecimal.class).optional().orElse(BigDecimal.ZERO);
        Integer asistentes = jdbc.sql("""
                SELECT COALESCE(SUM(COALESCE(r.asistentes,0)),0) FROM reserva r
                JOIN recurso rec ON rec.id=r.recurso_id
                WHERE r.estado='FINALIZADA' AND r.fecha BETWEEN :desde AND :hasta AND rec.tipo='SALA_MINICINE'
                """).param("desde",desde).param("hasta",hasta).query(Integer.class).single();
        BigDecimal consumos = jdbc.sql("""
                SELECT COALESCE(SUM(c.cantidad*c.precio_unitario),0) FROM consumo c
                JOIN reserva r ON r.id=c.reserva_id
                WHERE r.estado='FINALIZADA' AND r.fecha BETWEEN :desde AND :hasta
                """).param("desde",desde).param("hasta",hasta).query(BigDecimal.class).single();

        BigDecimal disponiblesBillar = BigDecimal.valueOf(horasAtencion).multiply(BigDecimal.valueOf(dias).multiply(BigDecimal.valueOf(billarRecursos)));
        BigDecimal disponiblesMinicine = BigDecimal.valueOf(horasAtencion).multiply(BigDecimal.valueOf(dias).multiply(BigDecimal.valueOf(minicineRecursos)));

        List<ReporteServicio> reportes = new ArrayList<>();
        reportes.add(new ReporteServicio("Billar", porcentaje(horasBillar, disponiblesBillar), ingresosBillar));
        reportes.add(new ReporteServicio("Minicine", porcentaje(bloquesMinicine, disponiblesMinicine),
                tarifaPersona.multiply(BigDecimal.valueOf(asistentes)).add(consumos)));
        return reportes;
    }

    private BigDecimal porcentaje(int ocupadas, BigDecimal disponibles) {
        if (disponibles.signum() == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(ocupadas).multiply(BigDecimal.valueOf(100)).divide(disponibles, 2, RoundingMode.HALF_UP);
    }
}
