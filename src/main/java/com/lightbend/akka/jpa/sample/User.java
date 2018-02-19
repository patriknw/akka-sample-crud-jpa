package com.lightbend.akka.jpa.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "USERS")
public class User {
  // primary key
  @Id
  @GeneratedValue(generator = "increment")
  @GenericGenerator(name = "increment", strategy = "increment")
  private long id;

  // optimistic locking
  @Version
  private long version;

  private final String name;
  private final int age;
  private final String countryOfResidence;

  User() {
    this.id = 0;
    this.version = 0;
    this.name = "";
    this.countryOfResidence = "";
    this.age = 1;
  }

  public User(String name, int age, String countryOfResidence) {
    this(0, 0, name, age, countryOfResidence);
  }

  public User(long id, long version, String name, int age, String countryOfResidence) {
    this.id = id;
    this.version = version;
    this.name = name;
    this.age = age;
    this.countryOfResidence = countryOfResidence;
  }

  public long getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  public String getCountryOfResidence() {
    return countryOfResidence;
  }

  @Transient
  @JsonIgnore
  boolean isNew() {
    return id <= 0;
  }



}
