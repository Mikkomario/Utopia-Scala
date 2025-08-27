package utopia.scribe.core.model.stored.management

import utopia.scribe.core.model.factory.management.ResolutionFactoryWrapper
import utopia.scribe.core.model.partial.management.ResolutionData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}

object Resolution extends StandardStoredFactory[ResolutionData, Resolution]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = ResolutionData
}

/**
  * Represents a resolution that has already been stored in the database
  * @param id   id of this resolution in the database
  * @param data Wrapped resolution data
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class Resolution(id: Int, data: ResolutionData) 
	extends StoredModelConvertible[ResolutionData] with FromIdFactory[Int, Resolution] 
		with ResolutionFactoryWrapper[ResolutionData, Resolution]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: ResolutionData) = copy(data = data)
}

