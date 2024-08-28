package utopia.logos.database.props.text

/**
  * Common trait for interfaces that provide access to text placement database properties by
  *  wrapping a TextPlacementDbProps
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbPropsWrapper extends TextPlacementDbProps
{
	// ABSTRACT	--------------------
	
	/**
	  * The wrapped text placement database properties
	  */
	protected def textPlacementDbProps: TextPlacementDbProps
	
	
	// IMPLEMENTED	--------------------
	
	override def id = textPlacementDbProps.id
	override def orderIndex = textPlacementDbProps.orderIndex
	override def parentId = textPlacementDbProps.parentId
	override def placedId = textPlacementDbProps.placedId
}

