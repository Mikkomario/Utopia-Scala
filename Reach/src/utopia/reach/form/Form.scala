package utopia.reach.form

import utopia.firmament.localization.Language.english
import utopia.firmament.localization.LocalString._
import utopia.firmament.localization.Localizer
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.combine.Combinable
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.{Pointer, Settable}
import utopia.flow.view.template.eventful.Flag
import utopia.reach.form.FormFieldOut.{Cancel, Delayed}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Form
{
	// COMPUTED --------------------------
	
	/**
	 * Creates a new form builder
	 * @param log Implicit logging implementation. Used in asynchronous result-handling and pointer management.
	 * @param exc Implicit execution context. Used when handling delayed form field outputs.
	 * @param showNotification Logic for displaying notifications
	 * @param localizer Localizer for displaying error messages
	 * @return A new form builder
	 */
	def newBuilder(implicit log: Logger, exc: ExecutionContext, showNotification: ShowFormNotification,
	               localizer: Localizer) =
		new FormBuilder()
	
	
	// OTHER    --------------------------
	
	/**
	 * @param field The first field in this form
	 * @param moreFields Other fields in this form
	 * @param log Implicit logging implementation. Used in asynchronous result-handling and pointer management.
	 * @param exc Implicit execution context. Used when handling delayed form field outputs.
	 * @param showNotification Logic for displaying notifications
	 * @param localizer Localizer for displaying error messages
	 * @return A new form
	 */
	def apply(field: FormField, moreFields: FormField*)
	         (implicit log: Logger, exc: ExecutionContext, showNotification: ShowFormNotification, localizer: Localizer): Form =
		new Form(field +: moreFields)
	/**
	 * @param fields The fields that form this form
	 * @param log Implicit logging implementation. Used in asynchronous result-handling and pointer management.
	 * @param exc Implicit execution context. Used when handling delayed form field outputs.
	 * @param showNotification Logic for displaying notifications
	 * @param localizer Localizer for displaying error messages
	 * @return A new form
	 */
	def apply(fields: Iterable[FormField])
	         (implicit log: Logger, exc: ExecutionContext, showNotification: ShowFormNotification, localizer: Localizer) =
		new Form(fields)
}

/**
 * Used for managing fillable forms, consisting of multiple input components and input validation procedures.
 * @param fields The fields that form this form
 * @param log Implicit logging implementation. Used in asynchronous result-handling and pointer management.
 * @param exc Implicit execution context. Used when handling delayed form field outputs.
 * @param showNotification Logic for displaying notifications
 * @param localizer Localizer for displaying error messages
 * @author Mikko Hilpinen
 * @since 07.09.2025, v1.7
 */
class Form(fields: Iterable[FormField])
          (implicit log: Logger, exc: ExecutionContext, showNotification: ShowFormNotification, localizer: Localizer)
	extends View[Future[Option[Model]]] with Combinable[FormField, Form]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	 * Contains the last generated long-running result future.
	 * Cleared occasionally.
	 */
	private val pendingResultP = Pointer.eventful.empty[Future[Option[Model]]]
	
	/**
	 * A flag that contains true while a value from this form has been requested, but not yet resolved
	 */
	lazy val loadingFlag = pendingResultP.flatMap {
		case Some(resultFuture) => !Flag.completionOf(resultFuture)
		case None => AlwaysFalse
	}
	
	
	// IMPLEMENTED  ---------------------------
	
	// Checks if a result future has already been queued
	override def value: Future[Option[Model]] = pendingResultP.updateAndGet { _.filterNot { _.isCompleted } }.getOrElse {
		// If not, starts processing the field output
		// Collects successfully generated values
		val successBuilder = OptimizedIndexedSeq.newBuilder[Constant]
		// Collects pending result futures
		val pendingBuilder = OptimizedIndexedSeq.newBuilder[(FormField, Lazy[Future[FormFieldOut]])]
		// This flag is set if any form field yields a canceling value (failure or cancel)
		// If set, causes this process to terminate and to yield None
		val canceledFlag = Settable()
		
		// Collects the primary output, until completed or canceled
		fields.iterator.takeWhile { _ => canceledFlag.isNotSet }.foreach { field =>
			handleOutput(field, field.value, successBuilder, pendingBuilder, canceledFlag)
		}
		
		// Case: Was canceled => Yields None
		if (canceledFlag.isSet)
			Future.successful(None)
		// Case: Not canceled (yet) => Checks if any results were delayed
		else {
			val pending = pendingBuilder.result()
			// Case: No delays => Yields the final model
			if (pending.isEmpty)
				Future.successful(Some(Model.withConstants(successBuilder.result())))
			// Case: Delays => Processes those asynchronously
			else {
				val resultFuture = Future {
					processPending(pending, successBuilder, canceledFlag)
					// Case: Canceled during pending processing => Yields None
					if (canceledFlag.isSet)
						None
					// Case: Not canceled => Yields the final model
					else
						Some(Model.withConstants(successBuilder.result()))
				}
				// Remembers the pending process, in case value is called again soon
				pendingResultP.setOne(resultFuture)
				resultFuture
			}
		}
	}
	
	override def +(other: FormField): Form = new Form(fields.toOptimizedSeq :+ other)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param fields Fields to add to this form
	 * @return A copy of this form with the specified fields included
	 */
	def ++(fields: IterableOnce[FormField]) = new Form(this.fields ++ fields)
	/**
	 * @param other Another form
	 * @return A form that contains fields from both these forms
	 */
	def ++(other: Form) = new Form(fields ++ other.fields)
	
	/**
	 * Processes pending form field output.
	 * Assumes that this function is called in an asynchronous thread.
	 * @param pending Pending output to process
	 * @param successBuilder A builder that accepts successfully acquired form values
	 * @param canceledFlag A flag that is set in case a field yields a canceling value
	 */
	@tailrec
	private def processPending(pending: Seq[(FormField, Lazy[Future[FormFieldOut]])],
	                           successBuilder: mutable.Growable[Constant], canceledFlag: Settable): Unit =
	{
		// Prepares for a potential next iteration / level
		val nextPendingBuilder = OptimizedIndexedSeq.newBuilder[(FormField, Lazy[Future[FormFieldOut]])]
		// Processes the results until canceled or until this level has completed
		pending.iterator.takeWhile { _ => canceledFlag.isNotSet }.foreach { case (field, lazyResultFuture) =>
			lazyResultFuture.value.waitFor() match {
				// Case: Output acquired => Processes it
				case Success(output) => handleOutput(field, output, successBuilder, nextPendingBuilder, canceledFlag)
				// Case: No output => Logs and displays the error. Simulates a successfully acquired empty value.
				case Failure(error) =>
					log(error, "Unexpected failure while processing a pending field output")
					showNotification("Unexpected failure while processing field output.\nError message: %s"
						.in(english).localized.interpolate(error.getMessage), field.component)
					successBuilder += Constant(field.name, Value.empty)
			}
		}
		
		// Checks whether to continue to the next level (recursively)
		val nextPending = nextPendingBuilder.result()
		if (nextPending.nonEmpty)
			processPending(nextPending, successBuilder, canceledFlag)
	}
	
	/**
	 * Handles the output value of a single form field
	 * @param field The form field
	 * @param output The acquired output
	 * @param successBuilder A builder for successfully acquired values
	 * @param pendingBuilder A builder for pending outputs
	 * @param canceledFlag A flag that should be set if a canceling value is encountered
	 */
	@tailrec
	private def handleOutput(field: FormField, output: FormFieldOut, successBuilder: mutable.Growable[Constant],
	                         pendingBuilder: mutable.Growable[(FormField, Lazy[Future[FormFieldOut]])],
	                         canceledFlag: Settable): Unit =
		output match {
			// Case: Success => Remembers the value
			case FormFieldOut.Success(value) => successBuilder += Constant(field.name, value)
			// Case: Failure => Displays a message, requests focus and cancels this process
			case FormFieldOut.Failure(message) =>
				field.focusable.foreach { _.requestFocus() }
				showNotification(message, field.component)
				canceledFlag.set()
			// Case: Cancel => Silently cancels this process
			case Cancel => canceledFlag.set()
			// Case: Delayed => Checks whether the result is immediate
			case Delayed(lazyResult) =>
				lazyResult.current.flatMap { _.current } match {
					// Case: Result immediately available => Handles it recursively
					case Some(immediate) => handleOutput(field, immediate, successBuilder, pendingBuilder, canceledFlag)
					// Case: Result is still pending => Remembers it for later
					case None => pendingBuilder += (field -> lazyResult)
				}
		}
}
