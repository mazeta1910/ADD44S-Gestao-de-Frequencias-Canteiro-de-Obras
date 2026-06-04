package br.edu.utfpr.service;

import br.edu.utfpr.dao.GenericDao;
import br.edu.utfpr.model.Canteiro;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CanteiroService {

    private final GenericDao<Canteiro> dao;
    private final EntityManager em;

    public CanteiroService(EntityManager em) {
        this.em = em;
        this.dao = new GenericDao<>(em, Canteiro.class);
    }

    public void inserir(Canteiro canteiro) {
        dao.inserir(canteiro);
    }

    public void atualizar(Canteiro canteiro) {
        dao.atualizar(canteiro);
    }

    public void alterar(Canteiro canteiro) {
        dao.atualizar(canteiro);
    }

    public void remover(Long id) {
        dao.remover(id);
    }

    public void excluir(Long id) {
        dao.remover(id);
    }

    public Canteiro buscarPorId(Long id) {
        return dao.buscarPorId(id);
    }

    public List<Canteiro> buscarTodos() {
        return dao.buscarTodos();
    }
}