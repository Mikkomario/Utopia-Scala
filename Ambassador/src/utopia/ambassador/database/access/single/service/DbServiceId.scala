package utopia.ambassador.database.access.single.service

import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.database.model.service.AuthServiceModel
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.column.SingleIntIdAccess
import utopia.vault.nosql.view.{FactoryView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing individual service ids in the DB
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object DbServiceId extends SingleIntIdAccess with FactoryView[AuthService] with UnconditionalView
{
	// COMPUTED --------------------------------
	
	private def model = AuthServiceModel
	
	
	// IMPLEMENTED  ----------------------------
	
	override def factory = AuthServiceFactory
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param serviceName A service name (match string)
	  * @param connection Implicit DB connection
	  * @return A service matching that name (case-insensitive). None if the name didn't match any service.
	  */
	def forName(serviceName: String)(implicit connection: Connection) =
		find(model.nameColumn.like(serviceName))
}
