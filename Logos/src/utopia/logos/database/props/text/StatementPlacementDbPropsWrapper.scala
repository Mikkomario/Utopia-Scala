package utopia.logos.database.props.text

/**
  * Common trait for interfaces that provide access to statement placement database properties by 
  * wrapping a StatementPlacementDbProps
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.3.1
  */
trait StatementPlacementDbPropsWrapper extends StatementPlacementDbProps
{
	// ABSTRACT	--------------------
	
	/**
	  * The wrapped statement placement database properties
	  */
	protected def statementPlacementDbProps: StatementPlacementDbProps
	
	
	// IMPLEMENTED	--------------------
	
	override def id = statementPlacementDbProps.id
	
	override def orderIndex = statementPlacementDbProps.orderIndex
	override def parentId = statementPlacementDbProps.parentId
	override def statementId = statementPlacementDbProps.statementId
}

