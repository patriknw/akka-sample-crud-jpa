package com.lightbend.akka.jpa.sample;

import akka.Done;
import scala.concurrent.ExecutionContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class UserRepository {
  // use withEntityManager
  private final EntityManagerFactory entityManagerFactory;
  private final ExecutionContext blockingDispatcher;

  public UserRepository(EntityManagerFactory entityManagerFactory, ExecutionContext blockingDispatcher) {
    this.entityManagerFactory = entityManagerFactory;
    this.blockingDispatcher = blockingDispatcher;
  }

  public CompletionStage<Optional<User>> findById(long id) {
    return withEntityManager(entityManager -> blockingFindById(entityManager, id));
  }

  // extracted because used from both findById and delete
  private Optional<User> blockingFindById(EntityManager entityManager, long id) {
    try {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<User> criteria = builder.createQuery(User.class);
      Root<User> root = criteria.from(User.class);
      criteria.select(root).where(builder.equal(root.get("id"), id));

      User result = entityManager.createQuery(criteria).getSingleResult();
      return Optional.ofNullable(result);

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CompletionStage<List<User>> findAll() {
    return withEntityManager(entityManager -> {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<User> criteria = builder.createQuery(User.class);
      Root<User> root = criteria.from(User.class);
      criteria.select(root);

      List<User> result = entityManager.createQuery(criteria).getResultList();
      return result;
    });
  }

  public CompletionStage<List<User>> findByName(String name) {
    return withEntityManager(entityManager -> {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<User> criteria = builder.createQuery(User.class);
      Root<User> root = criteria.from(User.class);
      criteria.select(root).where(builder.equal(root.get("name"), name));

      List<User> result = entityManager.createQuery(criteria).getResultList();
      return result;
    });
  }

  public CompletionStage<Done> save(User user) {
    return withEntityManager(entityManager -> {
      if (user.isNew()) {
        // new user
        entityManager.persist(user);
      } else {
        // update
        entityManager.merge(user);
      }
      return Done.getInstance();
    });
  }

  public CompletionStage<Done> delete(long id) {
    return withEntityManager(entityManager -> {
      Optional<User> maybeUser = blockingFindById(entityManager, id);
      if (maybeUser.isPresent())
        entityManager.remove(maybeUser.get());
      return Done.getInstance();
    });
  }

  /**
   * All data access should be performed via this method.
   *
   * It handles the lifecycle of the EntityManager, i.e. closing it after
   * the data access code returns or throws.
   *
   * A transaction is started before the data access code is executed and
   * it is committed afterwards  or rolled back if exception was thrown.
   *
   * The blocking data access code is executed in a dedicated Akka dispatcher
   * (thread pool) to avoid starvation of other non-blocking parts of the system.
   *
   * The `CompletionStage` is returned immediately and completed later when with the
   * value that the data access code returns. If exception is thrown by the entity manager,
   * the data access code, or transaction the `CompletionStage` is completed exceptionally
   * with that exception.
   */
  private <T> CompletionStage<T> withEntityManager(Function<EntityManager, T> dataAccess) {
    CompletableFuture<T> result = new CompletableFuture<>();
    blockingDispatcher.execute(() -> {
      try {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
          entityManager.getTransaction().begin();
          T r = dataAccess.apply(entityManager);
          entityManager.getTransaction().commit();
          entityManager.close();
          result.complete(r);
        } catch (Exception e) {
          try {
            EntityTransaction txn = entityManager.getTransaction();
            if (txn.isActive())
              txn.rollback();
            entityManager.close();
          } catch (Exception ignore) {
            // the original exception is more interesting
          }
          throw e;
        }
      } catch (Exception e) {
        result.completeExceptionally(e);
      }
    });
    return result;
  }
}
