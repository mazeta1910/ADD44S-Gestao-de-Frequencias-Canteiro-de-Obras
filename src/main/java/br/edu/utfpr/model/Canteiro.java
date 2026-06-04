package br.edu.utfpr.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "canteiro")
public class Canteiro implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 300)
    private String localizacao;

    @Column(nullable = false, length = 200)
    private String responsavel;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataPrevisaoTermino;

    public Canteiro() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataPrevisaoTermino() {
        return dataPrevisaoTermino;
    }

    public void setDataPrevisaoTermino(LocalDate dataPrevisaoTermino) {
        this.dataPrevisaoTermino = dataPrevisaoTermino;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("CANTEIRO\n");
        sb.append("=".repeat(60)).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("Nome: ").append(nome).append("\n");
        sb.append("Localização: ").append(localizacao).append("\n");
        sb.append("Responsável: ").append(responsavel).append("\n");
        sb.append("Data Início: ").append(dataInicio.format(formatter)).append("\n");
        sb.append("Previsão Término: ").append(dataPrevisaoTermino.format(formatter)).append("\n");
        sb.append("=".repeat(60));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Canteiro canteiro = (Canteiro) o;
        return id != null && id.equals(canteiro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
