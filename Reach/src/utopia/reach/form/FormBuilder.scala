package utopia.reach.form

import utopia.firmament.localization.Localizer
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.util.logging.Logger

import scala.collection.mutable
import scala.concurrent.ExecutionContext

/**
 * Used for collecting the fields that form a form
 *
 * @author Mikko Hilpinen
 * @since 08.09.2025, v1.7
 */
class FormBuilder()(implicit log: Logger, exc: ExecutionContext, showNotification: ShowFormNotification,
                    localizer: Localizer)
	extends mutable.Builder[FormField, Form]
{
	// ATTRIBUTES   -------------------------
	
	private val wrapped = OptimizedIndexedSeq.newBuilder[FormField]
	
	
	// IMPLEMENTED  -------------------------
	
	override def clear(): Unit = wrapped.clear()
	override def result(): Form = new Form(wrapped.result())
	
	override def addOne(elem: FormField): FormBuilder.this.type = {
		wrapped += elem
		this
	}
}
