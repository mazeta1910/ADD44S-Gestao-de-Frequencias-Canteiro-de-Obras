package br.edu.utfpr.service;

import br.edu.utfpr.dao.GenericDao;
import br.edu.utfpr.model.Trabalhador;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

public class TrabalhadorService {

    private final GenericDao<Trabalhador> dao;
    private final EntityManager em;

    public TrabalhadorService(EntityManager em) {
        this.em = em;
        this.dao = new GenericDao<>(em, Trabalhador.class);
    }

    public void inserir(Trabalhador trabalhador) {
        dao.inserir(trabalhador);
    }

    public void atualizar(Trabalhador trabalhador) {
        dao.atualizar(trabalhador);
    }

    public void alterar(Trabalhador trabalhador) {
        dao.atualizar(trabalhador);
    }

    public void remover(Long id) {
        dao.remover(id);
    }

    public void excluir(Long id) {
        dao.remover(id);
    }

    public Trabalhador buscarPorId(Long id) {
        return dao.buscarPorId(id);
    }

    public List<Trabalhador> buscarTodos() {
        return dao.buscarTodos();
    }

    public List<Trabalhador> filtrarPorFuncao(String funcao) {
        return buscarTodos().stream()
                .filter(t -> t.getFuncao().equalsIgnoreCase(funcao))
                .collect(Collectors.toList());
    }

    public Optional<Trabalhador> buscarPorCPF(String cpf) {
        return buscarTodos().stream()
                .filter(t -> t.getCpf().equals(cpf))
                .findFirst();
    }

    public Map<String, Long> contarPorTipoContrato() {
        return buscarTodos().stream()
                .collect(Collectors.groupingBy(
                        Trabalhador::getTipoContrato,
                        Collectors.counting()
                ));
    }

    public List<String> listarNomes() {
        return buscarTodos().stream()
                .map(Trabalhador::getNomeCompleto)
                .sorted()
                .collect(Collectors.toList());
    }

    public double calcularMediaSalarial() {
        return buscarTodos().stream()
                .mapToDouble(t -> 3000.0)
                .average()
                .orElse(0.0);
    }

    public List<Trabalhador> buscarComCREA() {
        return buscarTodos().stream()
                .filter(t -> t.getNumeroCREA() != null && !t.getNumeroCREA().isEmpty())
                .collect(Collectors.toList());
    }

    public Map<String, List<Trabalhador>> agruparPorFuncao() {
        return buscarTodos().stream()
                .collect(Collectors.groupingBy(Trabalhador::getFuncao));
    }

    public boolean existeCPF(String cpf) {
        return buscarTodos().stream()
                .anyMatch(t -> t.getCpf().equals(cpf));
    }

    public long contarTotal() {
        return buscarTodos().stream().count();
    }
}