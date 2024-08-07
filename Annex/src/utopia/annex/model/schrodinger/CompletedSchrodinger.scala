package utopia.annex.model.schrodinger

import utopia.flow.view.immutable.eventful.Fixed

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@deprecated("Replaced with a rewritten Schrodinger class at utopia.annex.schrodinger", "v1.4")
object CompletedSchrodinger
{
	/**
	  * @param result Server result
	  * @tparam R Type of server result
	  * @return A schrödinger completed with said result. The processed instance will match this result.
	  */
	@deprecated("Replaced with Schrodinger.resolved(...)", "v1.4")
	def apply[R](result: R) = new CompletedSchrodinger(result, result)
	
	/**
	  * @param result Successful result contents
	  * @tparam R Type of result contents
	  * @return A schrödinger completed with said result. Server response is specified result wrapped in Success(...)
	  */
	@deprecated("Replaced with Schrodinger.successful(...)", "v1.4")
	def success[R](result: R) = new CompletedSchrodinger[Try[R], R](Success(result), result)
}

/**
  * An immutable schrödinger item that has already been completed
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
  * @tparam R Type of result this schrödinger was completed with
  */
@deprecated("Replaced with a rewritten Schrodinger class at utopia.annex.schrodinger", "v1.4")
case class CompletedSchrodinger[+R, +I](result: R, override val instance: I) extends ShcrodingerLike[R, I]
{
	// IMPLEMENTED	--------------------------
	
	override val instancePointer = Fixed(instance)
	
	override def serverResultFuture(implicit exc: ExecutionContext) = Future.successful(result)
	
	override def finalInstanceFuture(implicit exc: ExecutionContext) = Future.successful(instance)
	
	override lazy val serverResultPointer = Fixed(Some(result))
}
