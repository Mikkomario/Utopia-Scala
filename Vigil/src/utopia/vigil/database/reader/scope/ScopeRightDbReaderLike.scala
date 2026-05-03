package utopia.vigil.database.reader.scope

import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.props.scope.ScopeRightDbProps

import scala.util.Success

import java.time.Instant

/**
  * Common trait for factories which parse scope right data from database-originated models
  * @tparam A Type of read instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbReaderLike[+A] extends DbRowReader[A] with ParseTableModel[A] with HasTableAsTarget
{
	// ABSTRACT	--------------------
	
	/**
	  * Database properties used when parsing column data
	  */
	def dbProps: ScopeRightDbProps
	
	/**
	  * @param model   Model from which additional data may be read
	  * @param id      Id to assign to the read/parsed scope right
	  * @param scopeId scope id to assign to the new scope right
	  * @param created created to assign to the new scope right
	  * @param usable  usable to assign to the new scope right
	  * @return A scope right with the specified data
	  */
	protected def apply(model: HasProperties, id: Int, scopeId: Int, created: Instant, usable: Boolean): A
	
	
	// IMPLEMENTED	--------------------
	
	override def fromValid(valid: Model) = 
		Success(apply(valid, valid(dbProps.id.name).getInt, scopeId = valid(dbProps.scopeId.name).getInt, 
			created = valid(dbProps.created.name).getInstant, usable = valid(dbProps.usable.name).getBoolean))
}

