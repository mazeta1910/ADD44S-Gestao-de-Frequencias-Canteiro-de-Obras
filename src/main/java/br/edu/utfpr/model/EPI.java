package br.edu.utfpr.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "epi")
public class EPI implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String numeroCA;

    @Column(nullable = false)
    private LocalDate dataEntrega;

    @Column(nullable = false)
    private LocalDate dataValidade;

    @ManyToOne
    @JoinColumn(name = "trabalhador_id")
    private Trabalhador trabalhador;

    public EPI() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNumeroCA() {
        return numeroCA;
    }

    public void setNumeroCA(String numeroCA) {
        this.numeroCA = numeroCA;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public Trabalhador getTrabalhador() {
        return trabalhador;
    }

    public void setTrabalhador(Trabalhador trabalhador) {
        this.trabalhador = trabalhador;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("EPI - EQUIPAMENTO DE PROTEÇÃO INDIVIDUAL\n");
        sb.append("=".repeat(60)).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("Tipo: ").append(tipo).append("\n");
        sb.append("Número CA: ").append(numeroCA).append("\n");
        sb.append("Data Entrega: ").append(dataEntrega.format(formatter)).append("\n");
        sb.append("Data Validade: ").append(dataValidade.format(formatter)).append("\n");
        
        if (dataValidade.isBefore(LocalDate.now())) {
            sb.append("⚠️  STATUS: VENCIDO\n");
        } else {
            sb.append("✅ STATUS: VÁLIDO\n");
        }
        
        if (trabalhador != null) {
            sb.append("Trabalhador: ").append(trabalhador.getNomeCompleto()).append("\n");
        }
        
        sb.append("=".repeat(60));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EPI epi = (EPI) o;
        return id != null && id.equals(epi.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
