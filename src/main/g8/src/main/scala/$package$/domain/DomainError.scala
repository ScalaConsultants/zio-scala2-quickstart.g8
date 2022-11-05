package $package$.domain

sealed trait DomainError {
  def asThrowable: Throwable = this match {
    case RepositoryError(cause)  => cause
    case ValidationError(msg)    => new Throwable(msg)
    case NotFoundError(maybeMsg) => maybeMsg.map(new Throwable(_)).getOrElse(new Throwable())
  }
}
final case class RepositoryError(cause: Exception) extends DomainError
final case class ValidationError(msg: String)                         extends DomainError
final case class NotFoundError private (maybeMessage: Option[String]) extends DomainError
object NotFoundError {
  def apply(msg: String): NotFoundError = NotFoundError(Some(msg))
  val empty: NotFoundError              = NotFoundError(None)
}
