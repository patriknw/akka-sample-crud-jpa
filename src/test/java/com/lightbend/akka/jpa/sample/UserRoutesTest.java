package com.lightbend.akka.jpa.sample;


//#test-top

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;

import scala.concurrent.ExecutionContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


//#set-up
public class UserRoutesTest extends JUnitRouteTest {
  //#test-top
  private ActorSystem system;
  private EntityManagerFactory entityManagerFactory;
  private TestRoute appRoute;

  @Before
  public void setUp() {
    system = ActorSystem.create("helloAkkaHttpServer");

    entityManagerFactory =
        Persistence.createEntityManagerFactory("akka-sample-crud-jpa");
    ExecutionContext blockingDispatcher =
        system.dispatchers().lookup("blocking-db-dispatcher");
    final UserRepository userRepository = new UserRepository(entityManagerFactory, blockingDispatcher);

    QuickstartServer server = new QuickstartServer(system, userRepository);
    appRoute = testRoute(server.createRoute());
  }

  @After
  public void tearDown() {
    entityManagerFactory.close();
    system.terminate();
  }

  //#set-up
  //#actual-test
  @Test
  public void testNoUsers() {
    appRoute.run(HttpRequest.GET("/users"))
        .assertStatusCode(StatusCodes.OK)
        .assertMediaType("application/json")
        .assertEntity("[]");
  }

  //#actual-test
  //#testing-post
  @Test
  public void testHandlePOST() {
    appRoute.run(HttpRequest.POST("/users")
        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
            "{\"name\": \"Kapi\", \"age\": 42, \"countryOfResidence\": \"jp\"}"))
        .assertStatusCode(StatusCodes.CREATED);

    User gotUser =
      appRoute.run(HttpRequest.GET("/users/1"))
        .assertStatusCode(StatusCodes.OK)
        .assertMediaType("application/json")
        .entity(Jackson.unmarshaller(User.class));
    assertEquals("Kapi", gotUser.getName());
    assertEquals(42, gotUser.getAge());
  }
  //#testing-post

  @Test
  public void testRemove() {
    appRoute.run(HttpRequest.DELETE("/users/1"))
        .assertStatusCode(StatusCodes.OK);
  }
  //#set-up
}
//#set-up
