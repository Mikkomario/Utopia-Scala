package utopia.annex.model.schrodinger
import utopia.flow.event.Changing

import scala.concurrent.{ExecutionContext, Future}

object CompletedSchrodinger
{
	/**
	  * @param result Server result
	  * @tparam R Type of server result
	  * @return A schrödinger completed with said result. The processed instance will match this result.
	  */
	def apply[R](result: R) = new CompletedSchrodinger(result, result)
}

/**
  * An immutable schrödinger item that has already been completed
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
  * @tparam R Type of result this schrödinger was completed with
  */
case class CompletedSchrodinger[R, I](result: R, override val instance: I) extends ShcrodingerLike[R, I]
{
	// IMPLEMENTED	--------------------------
	
	override val instancePointer = Changing.wrap(instance)
	
	override def serverResultFuture(implicit exc: ExecutionContext) = Future.successful(result)
	
	override def finalInstanceFuture(implicit exc: ExecutionContext) = Future.successful(instance)
	
	override lazy val serverResultPointer = Changing.wrap(Some(result))
}
