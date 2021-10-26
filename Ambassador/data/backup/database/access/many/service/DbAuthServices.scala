package utopia.ambassador.database.access.many.service

import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.access.many.model.ManyModelAccessById
import utopia.vault.nosql.view.RowFactoryView

/**
  * Used for accessing multiple authentication services at a time
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
object DbAuthServices extends ManyModelAccessById[AuthService, Int] with RowFactoryView[AuthService]
{
	override def factory = AuthServiceFactory
	override protected def defaultOrdering = None
	
	override def idToValue(id: Int) = id
}
