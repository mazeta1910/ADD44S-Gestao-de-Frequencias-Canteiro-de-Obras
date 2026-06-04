package br.edu.utfpr.service;

import br.edu.utfpr.dao.GenericDao;
import br.edu.utfpr.model.EPI;
import jakarta.persistence.EntityManager;
import java.util.List;

public class EPIService {

    private final GenericDao<EPI> dao;
    private final EntityManager em;

    public EPIService(EntityManager em) {
        this.em = em;
        this.dao = new GenericDao<>(em, EPI.class);  // ✅ AQUI!
    }

    public void inserir(EPI epi) {
        dao.inserir(epi);
    }

    public void atualizar(EPI epi) {
        dao.atualizar(epi);
    }

    public void alterar(EPI epi) {
        dao.atualizar(epi);
    }

    public void remover(Long id) {
        dao.remover(id);
    }

    public void excluir(Long id) {
        dao.remover(id);
    }

    public EPI buscarPorId(Long id) {
        return dao.buscarPorId(id);
    }

    public List<EPI> buscarTodos() {
        return dao.buscarTodos();
    }
}
