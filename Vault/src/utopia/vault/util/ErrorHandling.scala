package utopia.vault.util

import utopia.vault.error.{ErrorHandler, HandleError}

/**
  * This object determines how various errors are handled
  * @author Mikko Hilpinen
  * @since 18.7.2019, v1.3+
  */
@deprecated("Please use HandleError instead", "v2.0")
object ErrorHandling
{
	// COMPUTED	---------------------
	
	/**
	  * The default error handling principle used when no more specific principle has been defined. Defaults to Ignore.
	  */
	def defaultPrinciple = HandleError.default
	def defaultPrinciple_=(principle: ErrorHandler) = HandleError.default = principle
	
	/**
	  * @return The error handling principle used when model parsing fails
	  */
	def modelParsePrinciple = HandleError.duringRowParsing
	def modelParsePrinciple_=(principle: ErrorHandler) = HandleError.handleRowParseErrorsWith(principle)
	
	/**
	  * @return Error handling principle used when properties are not included in an insert because they
	  *         don't belong to a table, which may be result of a wrong property name definition
	  */
	//noinspection MutatorLikeMethodIsParameterless
	def insertClipPrinciple = HandleError.fromInsertClipping
	def insertClipPrinciple_=(principle: ErrorHandler) = HandleError.handleInsertClipErrorsWith(principle)
}
