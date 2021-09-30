package co.s4ncampus.fpwithscala.users.domain

sealed trait ValidationError extends Product with Serializable
case class UserAlreadyExistsError(user: User) extends ValidationError

//getUsuario//
//R// case class UserNotFoundError(user: User) extends ValidationError

