package utopia.reach.form

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.reach.form.FormFieldOut.{Cancel, Delayed, Failure, Success}

import scala.concurrent.Future

/**
 * Used for managing fillable forms, consisting of multiple input components and input validation procedures.
 *
 * @author Mikko Hilpinen
 * @since 07.09.2025, v1.7
 */
class Form(fields: Seq[FormField])(implicit log: Logger) extends View[Future[Option[Model]]]
{
	// ATTRIBUTES   ---------------------------
	
	private val pendingResultP = Pointer.eventful.empty[Future[Option[Model]]]
	
	
	// IMPLEMENTED  ---------------------------
	
	override def value: Future[Option[Model]] = pendingResultP.value.getOrElse {
		val successBuilder = OptimizedIndexedSeq.newBuilder[Constant]
		val pendingBuilder = OptimizedIndexedSeq.newBuilder[Lazy[Future[FormFieldOut]]]
		var canceled = false
		
		fields.foreach { field =>
			field.value match {
				case Success(value) => successBuilder += Constant(field.name, value)
				case Failure(message) =>
					// TODO: Display a pop-up
					canceled = true
				case Cancel => canceled = true
				case Delayed(lazyResult) => ???
			}
		}
		
		???
	}
}
