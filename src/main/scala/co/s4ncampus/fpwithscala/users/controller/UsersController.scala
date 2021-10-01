package co.s4ncampus.fpwithscala.users.controller

import co.s4ncampus.fpwithscala.users.domain._

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl


import org.http4s.{EntityDecoder, HttpRoutes}

import co.s4ncampus.fpwithscala.users.domain.User

class UsersController[F[_]: Sync] extends Http4sDsl[F] {

    implicit val userDecoder: EntityDecoder[F, User] = jsonOf

    private def createUser(userService: UserService[F]): HttpRoutes[F] = 
        HttpRoutes.of[F] {
            case req @ POST -> Root =>
                val action = for {
                    user <- req.as[User]
                    result <- userService.create(user).value
                } yield result
                
                action.flatMap {
                    case Right(saved) => Ok(saved.asJson)
                    case Left(UserAlreadyExistsError(existing)) => Conflict(s"The user with legal id ${existing.legalId} already exists")
                }
        }

    private def getUser(userService: UserService[F]): HttpRoutes[F] =
        HttpRoutes.of[F] {
            case GET -> Root / userLegalId =>
                val find = userService.getUser(userLegalId).value

                find.flatMap {
                    case Some(user) => Ok(user.asJson)
                    case None       => Conflict(s"The user with lega id ${userLegalId} does not exist")
                }
        }

    private def putUser(userService: UserService[F]): HttpRoutes[F] = 
        HttpRoutes.of[F] {
            case req @ PUT -> Root =>
                val action = for {
                    user  <- req.as[User]
                    result <- userService.putUser(user).value
                }yield result

                action.flatMap {
                    case Right(edited) => Ok(edited.asJson)
                    case Left(UserNotFoundError(userLegalId)) => Conflict(s"The user with legal id ${userLegalId} does not exists")
                }
        }

    private def deleteUser(userService: UserService[F]): HttpRoutes[F] =
        HttpRoutes.of[F] {
            case DELETE -> Root / userLegalId => 
                val find = userService.deleteUser(userLegalId).value
            
                find.flatMap {
                    case Right(_) => Ok("User Removed".asJson) //NoContent()
                    case Left(UserNotFoundError(userLegalId)) => Conflict(s"The user with legal id ${userLegalId} does not exists")
                }
                //    case None       => Conflict(s"The user with legal id ${userLegalId} does not exists")
                //}
        }
        

    def endpoints(userService: UserService[F]): HttpRoutes[F] = {
        //To convine routes use the function `<+>`
        createUser(userService) <+> getUser(userService) <+> putUser(userService) <+> deleteUser(userService)
    }

}

object UsersController {
    def endpoints[F[_]: Sync](userService: UserService[F]): HttpRoutes[F] =
        new UsersController[F].endpoints(userService)
}