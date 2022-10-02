package utopia.flow.util.logging

/**
  * A logger that ignores all errors and warnings
  * @author Mikko Hilpinen
  * @since 26.9.2022, v2.0
  */
object NoOpLogger extends Logger
{
	override def apply(error: Option[Throwable], message: String) = ()
}
