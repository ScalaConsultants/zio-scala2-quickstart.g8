package $package$.domain

sealed trait DomainError {
  def asThrowable: Throwable = this match {
    case RepositoryError(cause) => cause
    case ValidationError(msg)   => new Throwable(msg)
  }
}
case class RepositoryError(cause: Exception) extends DomainError
case class ValidationError(msg: String)      extends DomainError
