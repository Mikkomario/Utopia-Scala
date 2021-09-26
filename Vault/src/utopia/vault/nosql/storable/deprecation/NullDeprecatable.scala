package utopia.vault.nosql.storable.deprecation

import utopia.flow.datastructure.immutable.Value
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

/**
 * A common trait for model factories that support deprecation by utilizing a
 * nullable timestamp column that is null by default and
 * is set to current timestamp when an item gets deprecated
 * @author Mikko Hilpinen
 * @since 26.9.2021, v1.10
 */
trait NullDeprecatable[+M <: Storable] extends TimeDeprecatable
{
	// ABSTRACT   ----------------------------
	
	/**
	 * @param deprecation A deprecation timestamp
	 * @return A model that has the specified deprecation timestamp
	 */
	def withDeprecatedAfter(deprecation: Instant): M
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return Column that contains primary table index
	 */
	def idColumn = table.primaryColumn.get
	
	/**
	 * @return A model that has just been marked as deprecated
	 */
	def nowDeprecated = withDeprecatedAfter(Now)
	
	
	// IMPLEMENTED  --------------------------
	
	override def nonDeprecatedCondition = deprecationColumn.isNull
	
	
	// OTHER    ------------------------------
	
	/**
	 * Deprecates a single item
	 * @param id target item id
	 * @param connection Implicit DB Connection
	 * @return Whether the specified item was affected
	 */
	def deprecateId(id: Value)(implicit connection: Connection) =
		nowDeprecated.updateWhere(idColumn <=> id && nonDeprecatedCondition) > 0
	
	/**
	 * Deprecates multiple items
	 * @param ids Target ids
	 * @param connection Implicit DB Connection
	 * @return The number of affected items
	 */
	def deprecateIds(ids: Iterable[Value])(implicit connection: Connection) =
	{
		if (ids.isEmpty)
			0
		else
			nowDeprecated.updateWhere(idColumn.in(ids) && nonDeprecatedCondition)
	}
}
