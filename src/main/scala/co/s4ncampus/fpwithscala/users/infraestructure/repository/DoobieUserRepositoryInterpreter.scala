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

  def selectAll(): Query0[User] = sql"""
    SELECT ID, LEGAL_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE
    FROM USER
  """.query[User]

  def update(user: User): Query0[User] = {
    def getComma(c: String): String = if(c != "") "," else ""

    sql"""
      UPDATE USERS
      SET
        ${if(user.firstName != "")s"FIRST_NAME = '${user.firstName}'${getComma(user.lastName+user.email+user.phone)} "}
        ${if(user.lastName != "")s"LAST_NAME = '${user.lastName}'${getComma(user.email+user.phone)} "}
        ${if(user.email != "")s"EMAIL = '${user.email}'${getComma(user.phone)} "}
        ${if(user.phone != "")s"PHONE = '${user.phone}' "}
      WHERE LEGAL_ID = ${user.legalId}
    """.query[User]
  }

  def delete(legalId: String): Query0[User] = sql"""
    DELETE FROM USERS WHERE LEGAL_ID = $legalId
  """.query[User]

}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
    extends UserRepositoryAlgebra[F] {
  import UserSQL._

  def create(user: User): F[User] = 
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def findByLegalId(legalId: String): OptionT[F, User] = OptionT(selectByLegalId(legalId).option.transact(xa))

  def findAll(): OptionT[F, User] = OptionT(selectAll().option.transact(xa))

  //def update(user: User): F[User] = update(user).
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter[F](xa)
}