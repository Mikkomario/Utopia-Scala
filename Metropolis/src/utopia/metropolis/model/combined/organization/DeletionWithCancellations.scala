package utopia.metropolis.model.combined.organization

import utopia.flow.collection.value.typeless
import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.DeepExtender
import utopia.metropolis.model.partial.organization.DeletionData
import utopia.metropolis.model.stored.organization.{Deletion, DeletionCancel}

/**
  * Combines organization deletion & possible cancellation data
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
@deprecated("Replaced with OrganizationDeletionWithCancellations", "v2.0")
case class DeletionWithCancellations(deletion: Deletion, cancellations: Vector[DeletionCancel])
	extends DeepExtender[Deletion, DeletionData] with ModelConvertible
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether this deletion was cancelled
	  */
	def isCancelled = cancellations.nonEmpty
	
	
	// IMPLEMENTED	-------------------------
	
	override def wrapped = deletion
	
	override def toModel =
	{
		val base = deletion.toModel
		if (isCancelled)
			base + typeless.Constant("cancellations", cancellations.map { _.toModel })
		else
			base
	}
}
