package co.s4ncampus.fpwithscala.users.domain

import cats.data.EitherT

trait UserValidationAlgebra[F[_]] {
  /* Fails with a UserAlreadyExistsError */
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  //R//              Valor <- : -> Error


  /* Fails with a non found user legalId (userByLegalId) */
  //getUsuario// (userLegalId:String)
  //def foundById(user: User): EitherT[F, UserNotFoundError, Unit]

}