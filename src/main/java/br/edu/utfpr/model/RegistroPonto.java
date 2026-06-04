package br.edu.utfpr.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class RegistroPonto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Trabalhador trabalhador;

    private LocalDate dataRegistro;
    private LocalTime horaEntrada;
    private LocalTime horaSaidaIntervalo;
    private LocalTime horaRetornoIntervalo;
    private LocalTime horaSaida;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trabalhador getTrabalhador() { return trabalhador; }
    public void setTrabalhador(Trabalhador trabalhador) { this.trabalhador = trabalhador; }
    public LocalDate getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDate dataRegistro) { this.dataRegistro = dataRegistro; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }
    public LocalTime getHoraSaidaIntervalo() { return horaSaidaIntervalo; }
    public void setHoraSaidaIntervalo(LocalTime horaSaidaIntervalo) { this.horaSaidaIntervalo = horaSaidaIntervalo; }
    public LocalTime getHoraRetornoIntervalo() { return horaRetornoIntervalo; }
    public void setHoraRetornoIntervalo(LocalTime horaRetornoIntervalo) { this.horaRetornoIntervalo = horaRetornoIntervalo; }
    public LocalTime getHoraSaida() { return horaSaida; }
    public void setHoraSaida(LocalTime horaSaida) { this.horaSaida = horaSaida; }
}