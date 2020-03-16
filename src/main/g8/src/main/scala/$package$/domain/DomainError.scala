package $package$.domain

sealed trait DomainError
case class RepositoryError(cause: Exception) extends DomainError
case class ValidationError(msg: String)      extends DomainError
