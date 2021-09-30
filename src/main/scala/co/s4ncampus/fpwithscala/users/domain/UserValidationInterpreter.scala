package co.s4ncampus.fpwithscala.users.domain

import cats.Applicative
import cats.data.EitherT

class UserValidationInterpreter[F[_]: Applicative](repository: UserRepositoryAlgebra[F])
    extends UserValidationAlgebra[F] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] = 
    repository.findByLegalId(user.legalId).map(UserAlreadyExistsError).toLeft(())

  //-> Tenemos dudas en la estructura
  //getUsuario// (userLegalId:String) -> porque valida con el id, no con el User completo
  //def doesExist(userLegalId:String): EitherT[F, UserNotFoundError, Unit] =
    //repository.findByLegalId(userLegalId).map(UserNotFoundError).toLeft(())
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](repository: UserRepositoryAlgebra[F]) =
    new UserValidationInterpreter[F](repository)
}