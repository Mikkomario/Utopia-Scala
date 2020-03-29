package utopia.vault.util

import utopia.vault.util.ErrorHandlingPrinciple.Ignore

/**
  * This object determines how various errors are handled
  * @author Mikko Hilpinen
  * @since 18.7.2019, v1.3+
  */
object ErrorHandling
{
	// ATTRIBUTES	-----------------
	
	/**
	  * The default error handling principle used when no more specific principle has been defined. Defaults to Ignore.
	  */
	var defaultPrinciple: ErrorHandlingPrinciple = Ignore
	
	private var _modelParsePrinciple: Option[ErrorHandlingPrinciple] = None
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return The error handling principle used when model parsing fails
	  */
	def modelParsePrinciple = _modelParsePrinciple.getOrElse(defaultPrinciple)
	def modelParsePrinciple_=(principle: ErrorHandlingPrinciple) = _modelParsePrinciple = Some(principle)
}
