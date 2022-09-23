package utopia.ambassador.database.access.many.service

import utopia.ambassador.database.AmbassadorTables
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.column.ManyColumnAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple service names at a time
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
object DbAuthServiceNames extends ManyColumnAccess[String] with UnconditionalView with Indexed
{
	// ATTRIBUTES   --------------------------
	
	override lazy val column = table("name")
	
	
	// IMPLEMENTED  --------------------------
	
	override def table = AmbassadorTables.authService
	
	override def target = table
	
	override def parseValue(value: Value) = value.getString
	
	
	// OTHER    -----------------------------
	
	/**
	  * Finds the names matching the specified service ids
	  * @param serviceIds Service ids
	  * @param connection Implicit DB Connection
	  * @return Names of those services
	  */
	def forIds(serviceIds: Iterable[Int])(implicit connection: Connection) =
		find(index.in(serviceIds))
}
