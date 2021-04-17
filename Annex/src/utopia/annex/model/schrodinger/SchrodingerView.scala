package utopia.annex.model.schrodinger
import scala.concurrent.ExecutionContext

/**
  * A read only Schrödinger-wrapper
  * @author Mikko Hilpinen
  * @since 18.7.2020, v1
  */
class SchrodingerView[+R, +I](wrapped: ShcrodingerLike[R, I]) extends ShcrodingerLike[R, I]
{
	override def instancePointer = wrapped.instancePointer
	
	override def serverResultFuture(implicit exc: ExecutionContext) = wrapped.serverResultFuture
	
	override def finalInstanceFuture(implicit exc: ExecutionContext) = wrapped.finalInstanceFuture
	
	override def serverResultPointer = wrapped.serverResultPointer
}
