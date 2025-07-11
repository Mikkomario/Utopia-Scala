package utopia.logos.model.partial.text

/**
  * Common trait for classes which provide access to statement placement properties
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait HasStatementPlacementProps extends HasTextPlacementProps
{
	// ABSTRACT	--------------------
	
	/**
	  * Id of the statement which appears within the linked text
	  */
	def statementId: Int
	
	
	// IMPLEMENTED	--------------------
	
	override def placedId = statementId
}

