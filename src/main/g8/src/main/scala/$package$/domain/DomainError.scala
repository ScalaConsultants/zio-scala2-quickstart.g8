package $package$.domain

sealed trait DomainError {
  def asThrowable: Throwable = this match {
    case RepositoryError(cause) => cause
    case ValidationError(msg)   => new Throwable(msg)
    case NotFoundError(msg)     => new Throwable(msg)
  }
}
final case class RepositoryError(cause: Exception) extends DomainError
final case class ValidationError(msg: String) extends DomainError
final case class NotFoundError(msg: String)   extends DomainError
