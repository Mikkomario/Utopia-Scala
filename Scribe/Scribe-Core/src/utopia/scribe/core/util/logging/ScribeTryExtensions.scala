package utopia.scribe.core.util.logging

import utopia.flow.collection.CollectionExtensions.RichIterable
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.TryCatch
import utopia.scribe.core.controller.logging.Scribe
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Recoverable

import scala.util.{Failure, Success, Try}

/**
  * Contains extensions that apply to Tries and TryCatches
  * @author Mikko Hilpinen
  * @since 29.6.2023, v1.0
  */
object ScribeTryExtensions
{
	implicit class LoggableTry[+A](val t: Try[A]) extends AnyVal
	{
		/**
		  * Converts this try to another data type.
		  * Logs possible failure using a Scribe instance
		  * @param onSuccess Function that converts the successful value to the target data type
		  * @param onFailure Function that returns the result on failure
		  * @param log Function that accepts a scribe instance and the encountered failure
		  * @param scribe Implicit scribe implementation to use
		  * @tparam B Type of resulting value
		  * @return A converted version of this Try
		  */
		def scribeTo[B](onSuccess: A => B)(onFailure: => B)(log: (Scribe, Throwable) => Unit)
		               (implicit scribe: Scribe) =
			t match {
				case Success(v) => onSuccess(v)
				case Failure(error) =>
					log(scribe, error)
					onFailure
			}
		
		/**
		  * Converts this Try to an Option. Logs possible failure using a Scribe instance.
		  * @param log Function that accepts a scribe instance and the encountered error, in case of a failure
		  * @param scribe Implicit scribe implementation
		  * @return Some if this was a success. None otherwise.
		  */
		@deprecated("Deprecated for removal", "v1.1")
		def scribeToOption(log: (Scribe, Throwable) => Unit)(implicit scribe: Scribe): Option[A] =
			scribeTo[Option[A]] { Some(_) } { None }(log)
		
		/**
		  * Logs a potential failure in this Try using a Scribe instance
		  * @param log Function that accepts a scribe instance and the encountered error, in case of a failure
		  * @param scribe Implicit scribe implementation
		  */
		@deprecated("Deprecated for removal", "v1.1")
		def scribe(log: (Scribe, Throwable) => Unit)(implicit scribe: Scribe) = scribeTo { _ => () } { () }(log)
		
		/**
		  * Converts this Try to an Option. Logs possible failure using a Scribe instance.
		  * @param message Error message to include (optional)
		  * @param details Error details to include (optional, occurrence-specific)
		  * @param subContext Sub-context where this issue occurred (optional)
		  * @param severity Error severity (default = Unrecoverable)
		  * @param variantDetails Issue variant -specific details (optional)
		  * @param scribe Implicit scribe implementation
		  * @return Some if this was a success. None otherwise.
		  */
		@deprecated("Deprecated for removal. .logWith(...) now returns an Option, just like this function.", "v1.1")
		def logToOptionWith(message: String = "", details: Model = Model.empty, subContext: String = "",
		                    severity: Severity = Severity.default, variantDetails: Model = Model.empty)
		                   (implicit scribe: Scribe) =
			logWith(message, details, subContext, severity, variantDetails)
		
		/**
		  * Logs a potential failure in this Try using a Scribe instance
		  * @param message        Error message to include (optional)
		  * @param details        Error details to include (optional, occurrence-specific)
		  * @param subContext Sub-context where this issue occurred (optional)
		  * @param severity       Error severity (default = Unrecoverable)
		  * @param variantDetails Issue variant -specific details (optional)
		  * @param scribe         Implicit scribe implementation
		  * @return This Try as an option
		  */
		def logWith(message: String = "", details: Model = Model.empty, subContext: String = "",
		            severity: Severity = Severity.default, variantDetails: Model = Model.empty)
		           (implicit scribe: Scribe) =
			t match {
				case Failure(error) =>
					scribe.in(subContext)(severity).variant(variantDetails)(error, message, details)
					None
				case Success(v) => Some(v)
			}
	}
	
	implicit class LoggableTryCatch[+A](val t: TryCatch[A]) extends AnyVal
	{
		/**
		  * Converts this TryCatch into another data type.
		  * Logs potential failures using a Scribe instance.
		  * @param onSuccess Function that converts a successful value to the target data type
		  * @param onFailure Function that returns the value to return on a full failure
		  * @param log Function that accepts 3 parameters:
		  *                 1) A Scribe instance,
		  *                 2) An encountered error, and
		  *                 3) Whether this TryCatch is a full failure.
		  *            Also, the specified Scribe instance will have a preconfigured severity of Recoverable in case
		  *            of a partial failure.
		  * @param scr Implicit Scribe implementation to use
		  * @tparam B Type of result to yield
		  * @return Final result based on either failure or success
		  */
		def scribeTo[B](onSuccess: A => B)(onFailure: => B)
		               (log: (Scribe, Throwable, Boolean) => Unit)
		               (implicit scr: Scribe) =
			t match {
				case TryCatch.Success(v, failures) =>
					failures.groupBy { _.getClass }.valuesIterator
						.foreach { e => log(scr.partialFailure, e.head, false) }
					onSuccess(v)
				case TryCatch.Failure(error) =>
					log(scr, error, true)
					onFailure
			}
		
		/**
		  * Converts this TryCatch into a Try, logging potential encountered partial failures using a Scribe instance
		  * @param log A function that accepts a Scribe instance and an encountered error.
		  *            The Scribe instance will be configured with severity Recoverable
		  *            (as this function is only logging partial failures)
		  * @param scribe Implicit Scribe implementation
		  * @return Success in case of a full or partial success. Failure in case of a full failure.
		  */
		@deprecated("Deprecated for removal", "v1.1")
		def scribeToTry(log: (Scribe, Throwable) => Unit)(implicit scribe: Scribe) =
			t match {
				case TryCatch.Success(v, failures) =>
					failures.groupBy { _.getClass }.valuesIterator
						.foreach { e => log(scribe.partialFailure, e.head) }
					Success(v)
				case TryCatch.Failure(error) => Failure(error)
			}
		
		/**
		  * Converts this TryCatch into an Option,
		  * logging potential encountered failures using a Scribe instance
		  * @param log Function that accepts 3 parameters:
		  *            1) A Scribe instance,
		  *            2) An encountered error, and
		  *            3) Whether this TryCatch is a full failure.
		  *            Also, the specified Scribe instance will have a preconfigured severity of Recoverable in case
		  *            of a partial failure.
		  * @param scribe Implicit Scribe implementation
		  * @return Some in case of a full or partial success. None in case of a full failure.
		  */
		@deprecated("Deprecated for removal", "v1.1")
		def scribeToOption(log: (Scribe, Throwable, Boolean) => Unit)(implicit scribe: Scribe) =
			scribeTo[Option[A]] { Some(_) } { None }(log)
		
		/**
		  * Logs any encountered failures using a Scribe instance
		  * @param log    Function that accepts 3 parameters:
		  *               1) A Scribe instance,
		  *               2) An encountered error, and
		  *               3) Whether this TryCatch is a full failure.
		  *               Also, the specified Scribe instance will have a preconfigured severity of Recoverable in case
		  *               of a partial failure.
		  * @param scribe Implicit Scribe implementation
		  */
		@deprecated("Deprecated for removal", "v1.1")
		def scribe(log: (Scribe, Throwable, Boolean) => Unit)(implicit scribe: Scribe) =
			scribeTo { _ => () } { () }(log)
		
		/**
		  * Converts this TryCatch into a Try, logging potential encountered partial failures using a Scribe instance
		  * @param message Message to include in logging entries (optional)
		  * @param details        Error details to include (optional, occurrence-specific)
		  * @param subContext Sub-context where this issue occurred (optional)
		  * @param severity       Error severity (default = Recoverable)
		  * @param variantDetails Issue variant -specific details (optional)
		  * @param scribe Implicit Scribe implementation
		  * @return Success in case of a full or partial success. Failure in case of a full failure.
		  */
		def logToTryWith(message: String = "", details: Model = Model.empty, subContext: String = "",
		                 severity: Severity = Recoverable, variantDetails: Model = Model.empty)
		                (implicit scribe: Scribe) =
		{
			t match {
				case TryCatch.Success(v, failures) =>
					failures.groupBy { _.getClass }.valuesIterator.foreach { e =>
						val failureCountProp = if (e.hasSize > 1) Some(Constant("failureCount", e.size)) else None
						scribe.in(subContext)(severity).variant(variantDetails)
							.apply(e.head, message, details ++ failureCountProp)
					}
					Success(v)
				case TryCatch.Failure(error) => Failure(error)
			}
		}
		
		/**
		  * Converts this TryCatch into an Option,
		  * logging potential encountered failures using a Scribe instance
		  * @param message        Message to include in logging entries (optional)
		  * @param details        Error details to include (optional, occurrence-specific)
		  * @param subContext Sub-context where this issue occurred (optional)
		  * @param variantDetails Issue variant -specific details (optional)
		  * @param scribe         Implicit Scribe implementation
		  * @return Some in case of a full or partial success. None in case of a full failure.
		  */
		def logWith(message: String = "", details: Model = Model.empty, subContext: String = "",
		            variantDetails: Model = Model.empty)
		           (implicit scribe: Scribe) =
			scribeTo[Option[A]] { Some(_) } { None } {
				(s, e, _) => s.in(subContext).variant(variantDetails)(e, message, details) }
		
		/**
		  * Converts this TryCatch into an Option,
		  * logging potential encountered failures using a Scribe instance
		  * @param message        Message to include in logging entries (optional)
		  * @param details        Error details to include (optional, occurrence-specific)
		  * @param subContext Sub-context where this issue occurred (optional)
		  * @param variantDetails Issue variant -specific details (optional)
		  * @param scribe         Implicit Scribe implementation
		  * @return Some in case of a full or partial success. None in case of a full failure.
		  */
		@deprecated("Renamed to .logWith(...)", "v1.1")
		def logToOptionWith(message: String = "", details: Model = Model.empty, subContext: String = "",
		                    variantDetails: Model = Model.empty)
		                   (implicit scribe: Scribe) =
			scribeToOption { (s, e, _) => s.in(subContext).variant(variantDetails)(e, message, details) }
	}
}
