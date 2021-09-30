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
    //for {
    //  _ <- validation.doesExist(userLegalId)
    //  found <- EitherT.liftF((repository.findByLegalId(userLegalId).flatMap{
    //    case Some(user) => user
    //    case None       => None
    //  }))
    //} yield found
  
  def putUser(user: User)(implicit M: Monad[F]): EitherT[F, UserNotFoundError, User] =
    for {
      _ <- validation.doesExist(user.legalId)
      edited <- EitherT.liftF(repository.edit(user))
    } yield edited
}


object UserService{
  def apply[F[_]](
                 repositoryAlgebra: UserRepositoryAlgebra[F],
                 validationAlgebra: UserValidationAlgebra[F],
                 ): UserService[F] =
    new UserService[F](repositoryAlgebra, validationAlgebra)
}