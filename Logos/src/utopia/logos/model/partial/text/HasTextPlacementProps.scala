package utopia.logos.model.partial.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.logos.model.template.Placed

/**
  * Common trait for classes which provide access to text placement properties
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait HasTextPlacementProps extends ModelConvertible with Placed
{
	// ABSTRACT	--------------------
	
	/**
	  * Id of the text where the placed text appears
	  */
	def parentId: Int
	/**
	  * Id of the text that is placed within the parent text
	  */
	def placedId: Int
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("parentId" -> parentId, "placedId" -> placedId, "orderIndex" -> orderIndex))
}

