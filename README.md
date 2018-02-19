# Sample of CRUD application with JPA and Akka

## How to run

```
mvn compile exec:java
```

or

```
sbt run
```

Create user:

```
curl -H "Content-type: application/json" -X POST -d '{"name": "MrX", "age": 31, "countryOfResidence": "Canada"}' http://localhost:8080/users
```

List all users:

```
curl http://localhost:8080/users
```

Update user, version field is for optimistic locking:

```
curl -H "Content-type: application/json" -X POST -d '{"name": "MrX", "age": 32, "countryOfResidence": "Canada", "id": 1, "version": 0}' http://localhost:8080/users
```

Get user by id:

```
curl http://localhost:8080/users/1
```

Get user by name:

```
curl http://localhost:8080/users?name=MrX
```

Delete user by id:

```
curl -X DELETE http://localhost:8080/users/1
```
