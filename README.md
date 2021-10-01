#Functional Programming With Scala
Functional Programming With Scala is an API for user management, made with CQRS architecture and Scala Build Tool (SBT). It allows users to create a slot with their information. Also provides the option for deleting and uploading such data.

**Version 1.1**

##Contributors
- Hamilton Ricaurte Cárdenas  <hamiltonricaurte@seven4n.com>
- Ricardo Quintero Jiménez    <ricardoquintero@seven4n.com>
- Santiago Suaza Zapata       <santiagosuaza@seven4n.com>
- Diego Andrés Estrada Rojas  <diegoestrada@seven4n.com>

## Run/Implementation
- Open terminal and go to `main project folder`
- Execute `sbt` command to start Scala Build Tool.
- Execute `compile` command in order to asure everything's working.
- Execute `run` command.
- Now you can `Create/Update/Delete` your user and user data.

## Description:

## 1. Data base support 
In order for database to work in a functional way we chose Doobie, which transforms programs into computations that actually can be executed.
```scala
val CatsVersion = "2.2.0"
val Http4sVersion = "0.21.16"
val CirceVersion = "0.13.0"
val CirceConfigVersion = "0.8.0"
val CirceGenericExtrasVersion = "0.13.0"
val DoobieVersion = "0.9.2"
val H2Version = "1.4.200"
val MunitVersion = "0.7.20"
val LogbackVersion = "1.2.3"
val MunitCatsEffectVersion = "0.13.0"
val FlywayVersion = "7.2.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.s4ncampus",
    name := "users",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.0",
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-core"            % CatsVersion,
      "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client"  % Http4sVersion,
      "org.http4s"      %% "http4s-circe"         % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"           % Http4sVersion,
      "io.circe"        %% "circe-core"           % CirceVersion,
      "io.circe"        %% "circe-generic"        % CirceVersion,
      "io.circe"        %% "circe-parser"         % CirceVersion,
      "io.circe"        %% "circe-config"         % CirceConfigVersion,
      "io.circe"        %% "circe-generic-extras" % CirceGenericExtrasVersion,
      "org.tpolecat"    %% "doobie-core"          % DoobieVersion,
      "org.tpolecat"    %% "doobie-h2"            % DoobieVersion,
      "org.tpolecat"    %% "doobie-hikari"        % DoobieVersion,
      "org.tpolecat"    %% "doobie-postgres"      % DoobieVersion,
      "com.h2database"  %  "h2"                   % H2Version,
      "org.flywaydb"    %  "flyway-core"          % FlywayVersion,
      "org.scalameta"   %% "munit"                % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect-2"  % MunitCatsEffectVersion % Test,
      "org.tpolecat"    %% "doobie-scalatest"     % DoobieVersion          % Test,
      "ch.qos.logback"  %  "logback-classic"      % LogbackVersion,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
  
```

## 2. Apply database migrations
First we setup our database
```scala
CREATE TABLE USERS (
  ID BIGSERIAL PRIMARY KEY,
  LEGAL_ID VARCHAR NOT NULL,
  FIRST_NAME VARCHAR NOT NULL,
  LAST_NAME VARCHAR NOT NULL,
  EMAIL VARCHAR NOT NULL,
  PHONE VARCHAR NOT NULL
);
```
Then we create a `logback.xml` file
```scala
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <!-- On Windows machines setting withJansi to true enables ANSI
         color code interpretation by the Jansi library. This requires
         org.fusesource.jansi:jansi:1.8 on the class path.  Note that
         Unix-based operating systems such as Linux and Mac OS X
         support ANSI color codes by default. -->
    <withJansi>true</withJansi>
    <encoder>
      <pattern>[%thread] %highlight(%-5level) %cyan(%logger{15}) - %msg %n</pattern>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```
Finally we setup our databes configuration file
```scala
users {
  db {
    url="jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    user="sa"
    password=""
    driver="org.h2.Driver"
    connections = {
      poolSize = 5
    }
  }
  server {
    host="0.0.0.0"
    port=8000
  }
}

```

## 3. Add data base interpreter
We create a data base infrastructure repository interpreter for managin data and query proccessing.
```scala
package co.s4ncampus.fpwithscala.users.infraestructure.repository

import co.s4ncampus.fpwithscala.users.domain._

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import cats.effect.Bracket

private object UserSQL {

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (LEGAL_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE)
    VALUES (${user.legalId}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.phone})
  """.update

  def selectByLegalId(legalId: String): Query0[User] = sql"""
    SELECT ID, LEGAL_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE
    FROM USERS
    WHERE LEGAL_ID = $legalId
  """.query[User]

  def update(user: User): Update0 = {
    sql"""
      UPDATE USERS 
      SET 
        FIRST_NAME = ${user.firstName},
        LAST_NAME = ${user.lastName},
        EMAIL = ${user.email},
        PHONE = ${user.phone}
      WHERE LEGAL_ID = ${user.legalId}
    """.update
  }

  def delete(legalId: String): Update0 = sql"""
    DELETE FROM USERS WHERE LEGAL_ID = $legalId
  """.update
}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
    extends UserRepositoryAlgebra[F] {
  import UserSQL._

  def create(user: User): F[User] = 
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def findByLegalId(legalId: String): OptionT[F, User] = OptionT(selectByLegalId(legalId).option.transact(xa))

  def edit(user: User): F[User] = {
    update(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)
  }

  def remove(legalId: String): F[Boolean] = delete(legalId).run.transact(xa).map(cols => if(cols == 1) true else false)}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter[F](xa)
}
```
## 4. Add domain, algebra and interpreter
First we add our domain object `user` 
```scala
package co.s4ncampus.fpwithscala.users.domain

case class User(
    id: Option[Long],
    legalId: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String)
```
Then, an algebraic data type for user validation (`userValidationAlgebra`)
```scala
package co.s4ncampus.fpwithscala.users.domain

import cats.data.OptionT

trait UserRepositoryAlgebra[F[_]] {
  def create(user: User): F[User]
  def findByLegalId(legalId: String): OptionT[F, User]
  def edit(user: User):F[User]
}
```
And finally user repository(`userValidationInterpreter`)
```scala
package co.s4ncampus.fpwithscala.users.domain

import cats.Applicative
import cats.data.EitherT

class UserValidationInterpreter[F[_]: Applicative](repository: UserRepositoryAlgebra[F])
    extends UserValidationAlgebra[F] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] = 
    repository.findByLegalId(user.legalId).map(UserAlreadyExistsError).toLeft(())

  //-> Tenemos dudas en la estructura
  //getUsuario// (userLegalId:String) -> porque valida con el id, no con el User completo
  def doesExist(userLegalId:String): EitherT[F, UserNotFoundError, User] =
    repository.findByLegalId(userLegalId).toRight(UserNotFoundError(userLegalId))
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](repository: UserRepositoryAlgebra[F]) =
    new UserValidationInterpreter[F](repository)
}
```
## 5. Validation
We create validations for inspecting results
```scala
package co.s4ncampus.fpwithscala.users.domain

sealed trait ValidationError extends Product with Serializable
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserNotFoundError(userLegalId:String) extends ValidationError


```
## 6. Service 
The service is the entry point to our domain. It works with the provided repository and validation algebras to omplement behavior.
```scala
package co.s4ncampus.fpwithscala.users.domain

import cats.data._
import cats.Monad

class UserService[F[_]](repository: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {
  def create(user: User)(implicit M: Monad[F]): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(repository.create(user))
    } yield saved
  
  def getUser(userLegalId: String): OptionT[F, User] =
    repository.findByLegalId(userLegalId)
  
  def putUser(user: User)(implicit M: Monad[F]): EitherT[F, UserNotFoundError, User] =
    for {
      _ <- validation.doesExist(user.legalId)
      edited <- EitherT.liftF(repository.edit(user))
    } yield edited
  
  def deleteUser(userLegalId: String)(implicit M: Monad[F]): EitherT[F, UserNotFoundError, Boolean] =
    for {
      _ <- validation.doesExist(userLegalId)
      removed <- EitherT.liftF(repository.remove(userLegalId))
    } yield removed   
    
}

object UserService{
  def apply[F[_]](
                 repositoryAlgebra: UserRepositoryAlgebra[F],
                 validationAlgebra: UserValidationAlgebra[F],
                 ): UserService[F] =
    new UserService[F](repositoryAlgebra, validationAlgebra)
}
```
## 7. Endpoint
We created endpoints for different methos using the provided `UserService`.
```scala
    def endpoints(userService: UserService[F]): HttpRoutes[F] = {
        //To convine routes use the function `<+>`
        createUser(userService) <+> getUser(userService) <+> putUser(userService) <+> deleteUser(userService)
    }

}

object UsersController {
    def endpoints[F[_]: Sync](userService: UserService[F]): HttpRoutes[F] =
        new UsersController[F].endpoints(userService)
}
```




