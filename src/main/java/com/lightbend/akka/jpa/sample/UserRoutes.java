package com.lightbend.akka.jpa.sample;

import akka.Done;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;

import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Routes can be defined in separated classes like shown in here
 */
//#user-routes-class
public class UserRoutes extends AllDirectives {
  //#user-routes-class
  final private LoggingAdapter log;
  final private UserRepository userRepository;

  public UserRoutes(ActorSystem system, UserRepository userRepository) {
    log = Logging.getLogger(system, this);
    this.userRepository = userRepository;
  }

  /**
   * This method creates one route (of possibly many more that will be part of your Web App)
   */
  //#all-routes
  //#users-get-delete
  public Route routes() {
    return route(pathPrefix("users", () ->
        route(
            getOrPostUsers(),
            path(PathMatchers.longSegment(), id -> route(
                getUser(id),
                deleteUser(id)
                )
            )
        )
    ));
  }
  //#all-routes

  //#users-get-delete

  //#users-get-delete
  private Route getUser(long id) {
    return get(() -> {
      //#retrieve-user-info
      CompletionStage<Optional<User>> maybeUser = userRepository.findById(id);

      return onSuccess(() -> maybeUser,
          result -> {
            if (result.isPresent())
              return complete(StatusCodes.OK, result.get(), Jackson.<User>marshaller());
            else
              return complete(StatusCodes.NOT_FOUND);
          }
      );
      //#retrieve-user-info
    });
  }

  private Route deleteUser(long id) {
    return
        //#users-delete-logic
        delete(() -> {
          CompletionStage<Done> userDeleted = userRepository.delete(id);

          return onSuccess(() -> userDeleted,
              result -> {
                log.info("Deleted user [{}]", id);
                return complete(StatusCodes.OK);
              }
          );
        });
    //#users-delete-logic
  }
  //#users-get-delete

  //#users-get-post
  private Route getOrPostUsers() {
    return pathEnd(() ->
        route(
            get(() -> {
              return parameter("name", name -> {
                CompletionStage<List<User>> futureUsers = userRepository.findByName(name);
                return onSuccess(() -> futureUsers,
                    users -> complete(StatusCodes.OK, users, Jackson.marshaller()));
              });
            }),
            get(() -> {
              CompletionStage<List<User>> futureUsers = userRepository.findAll();
              return onSuccess(() -> futureUsers,
                  users -> complete(StatusCodes.OK, users, Jackson.marshaller()));
            }),
            post(() ->
                entity(
                    Jackson.unmarshaller(User.class),
                    user -> handleExceptions(optimisticLockingHandler, () -> {
                      boolean isNew = user.isNew();
                      CompletionStage<Done> userCreated = userRepository.save(user);
                      return onSuccess(() -> userCreated,
                          result -> {
                            if (isNew) {
                              log.info("Created user [{}]", user.getId());
                              return complete(StatusCodes.CREATED);
                            } else {
                              log.info("Updated user [{}]", user.getId());
                              return complete(StatusCodes.OK);
                            }
                          });
                    })))
        )
    );
  }

  final ExceptionHandler optimisticLockingHandler = ExceptionHandler.newBuilder()
      .match(OptimisticLockException.class, e ->
          complete(StatusCodes.CONFLICT, e.getMessage()))
      .build();

  //#users-get-post
}
