package br.edu.utfpr.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

public class GenericDao<T> {

    private final EntityManager em;
    private final Class<T> entityClass;

    public GenericDao(EntityManager em, Class<T> entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    public void inserir(T entity) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erro ao inserir: " + e.getMessage(), e);
        }
    }

    public void atualizar(T entity) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erro ao atualizar: " + e.getMessage(), e);
        }
    }

    public void remover(Long id) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T entity = em.find(entityClass, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erro ao remover: " + e.getMessage(), e);
        }
    }

    public T buscarPorId(Long id) {
        try {
            return em.find(entityClass, id);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar por ID: " + e.getMessage(), e);
        }
    }

    public List<T> buscarTodos() {
        try {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
            return em.createQuery(jpql, entityClass).getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar todos: " + e.getMessage(), e);
        }
    }
}