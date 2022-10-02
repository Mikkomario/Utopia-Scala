package utopia.citadel.database.access.single.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.citadel.database.model.description.DescriptionRoleModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct DescriptionRoles.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueDescriptionRoleAccess 
	extends SingleRowModelAccess[DescriptionRole] 
		with DistinctModelAccess[DescriptionRole, Option[DescriptionRole], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Key used in json documents for a singular value (string) of this description role. None if no instance (or value) was found.
	  */
	def jsonKeySingular(implicit connection: Connection) = pullColumn(model.jsonKeySingularColumn).string
	
	/**
	  * Key used in json documents for multiple values (array) of this description role. None if no instance (or value) was found.
	  */
	def jsonKeyPlural(implicit connection: Connection) = pullColumn(model.jsonKeyPluralColumn).string
	
	/**
	  * Time when this DescriptionRole was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionRoleFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted DescriptionRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the jsonKeyPlural of the targeted DescriptionRole instance(s)
	  * @param newJsonKeyPlural A new jsonKeyPlural to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def jsonKeyPlural_=(newJsonKeyPlural: String)(implicit connection: Connection) = 
		putColumn(model.jsonKeyPluralColumn, newJsonKeyPlural)
	
	/**
	  * Updates the jsonKeySingular of the targeted DescriptionRole instance(s)
	  * @param newJsonKeySingular A new jsonKeySingular to assign
	  * @return Whether any DescriptionRole instance was affected
	  */
	def jsonKeySingular_=(newJsonKeySingular: String)(implicit connection: Connection) = 
		putColumn(model.jsonKeySingularColumn, newJsonKeySingular)
}

