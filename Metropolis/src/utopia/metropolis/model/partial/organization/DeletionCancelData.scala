package utopia.metropolis.model.partial.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Contains basic information about an organization deletion cancellation
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  * @param deletionId Id of the cancelled deletion
  * @param creatorId Id of the user who cancelled the deletion (if known)
  */
case class DeletionCancelData(deletionId: Int, creatorId: Option[Int] = None) extends ModelConvertible
{
	override def toModel = Model(Vector("deletion_id" -> deletionId, "creator_id" -> creatorId))
}
