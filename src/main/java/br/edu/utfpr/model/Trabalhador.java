package br.edu.utfpr.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "trabalhador")
public class Trabalhador implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, length = 100)
    private String funcao;

    @Column(nullable = false)
    private LocalDate dataContratacao;

    @Column(nullable = false, length = 50)
    private String tipoContrato;

    @Column(length = 20)
    private String numeroCREA;

    @Column(length = 20)
    private String numeroRegistroProfissional;

    @Column(length = 100)
    private String especialidade;

    @ManyToOne
    @JoinColumn(name = "canteiro_id")
    private Canteiro canteiroAtual;

    public Trabalhador() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getFuncao() {
        return funcao;
    }

    public void setFuncao(String funcao) {
        this.funcao = funcao;
    }

    public LocalDate getDataContratacao() {
        return dataContratacao;
    }

    public void setDataContratacao(LocalDate dataContratacao) {
        this.dataContratacao = dataContratacao;
    }

    public String getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(String tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getNumeroCREA() {
        return numeroCREA;
    }

    public void setNumeroCREA(String numeroCREA) {
        this.numeroCREA = numeroCREA;
    }

    public String getNumeroRegistroProfissional() {
        return numeroRegistroProfissional;
    }

    public void setNumeroRegistroProfissional(String numeroRegistroProfissional) {
        this.numeroRegistroProfissional = numeroRegistroProfissional;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public Canteiro getCanteiroAtual() {
        return canteiroAtual;
    }

    public void setCanteiroAtual(Canteiro canteiroAtual) {
        this.canteiroAtual = canteiroAtual;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("TRABALHADOR\n");
        sb.append("=".repeat(60)).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("Nome: ").append(nomeCompleto).append("\n");
        sb.append("CPF: ").append(cpf).append("\n");
        sb.append("Função: ").append(funcao).append("\n");
        sb.append("Data Contratação: ").append(dataContratacao.format(formatter)).append("\n");
        sb.append("Tipo Contrato: ").append(tipoContrato).append("\n");
        
        if (numeroCREA != null && !numeroCREA.isEmpty()) {
            sb.append("CREA: ").append(numeroCREA).append("\n");
        }
        
        if (numeroRegistroProfissional != null && !numeroRegistroProfissional.isEmpty()) {
            sb.append("Registro Profissional: ").append(numeroRegistroProfissional).append("\n");
        }
        
        if (especialidade != null && !especialidade.isEmpty()) {
            sb.append("Especialidade: ").append(especialidade).append("\n");
        }
        
        if (canteiroAtual != null) {
            sb.append("Canteiro Atual: ").append(canteiroAtual.getNome()).append("\n");
        }
        
        sb.append("=".repeat(60));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trabalhador that = (Trabalhador) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private boolean administrador;

    public boolean isAdministrador() {
        return administrador;
    }

    public void setAdministrador(boolean administrador) {
        this.administrador = administrador;
    }
}
