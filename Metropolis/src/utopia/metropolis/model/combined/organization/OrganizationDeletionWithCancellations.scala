package utopia.metropolis.model.combined.organization

import utopia.flow.collection.value.typeless
import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.partial.organization.OrganizationDeletionData
import utopia.metropolis.model.stored.organization.{OrganizationDeletion, OrganizationDeletionCancellation}

/**
  * Combines deletion with cancellations data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionWithCancellations(deletion: OrganizationDeletion, 
	cancellations: Vector[OrganizationDeletionCancellation]) 
	extends Extender[OrganizationDeletionData] with ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this deletion in the database
	  */
	def id = deletion.id
	
	/**
	  * @return Whether this deletion was cancelled
	  */
	def isCancelled = cancellations.nonEmpty
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = deletion.data
	
	override def toModel =
	{
		val base = deletion.toModel
		if (isCancelled)
			base + typeless.Constant("cancellations", cancellations.map { _.toModel })
		else
			base
	}
}

