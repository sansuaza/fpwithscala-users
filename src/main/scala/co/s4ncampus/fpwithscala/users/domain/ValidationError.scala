package co.s4ncampus.fpwithscala.users.domain

sealed trait ValidationError extends Product with Serializable
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserNotFoundError(userLegalId:String) extends ValidationError

