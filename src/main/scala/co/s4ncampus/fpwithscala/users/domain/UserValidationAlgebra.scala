package co.s4ncampus.fpwithscala.users.domain

import cats.data.EitherT

trait UserValidationAlgebra[F[_]] {
  /* Fails with a UserAlreadyExistsError */
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  /* Fails with a non found user legalId (userByLegalId) */
  def doesExist(userLegalId:String): EitherT[F, UserNotFoundError, User]

}