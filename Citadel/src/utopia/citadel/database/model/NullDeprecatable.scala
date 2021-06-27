package utopia.citadel.database.model

import utopia.flow.datastructure.immutable.Value
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{DataDeletionRule, Storable, Table}
import utopia.vault.nosql.factory.Deprecatable
import utopia.vault.sql.SqlExtensions._

import java.time.Instant
import scala.concurrent.duration.FiniteDuration


/**
 * A common trait for model factories that support deprecation by utilizing a
 * nullable timestamp column that is null by default and
 * is set to current timestamp when an item gets deprecated
 * @author Mikko Hilpinen
 * @since 27.6.2021, v1.0
 */
trait NullDeprecatable[+M <: Storable] extends Deprecatable
{
	// ABSTRACT   ----------------------------
	
	/**
	 * @return The table used by this class
	 */
	def table: Table
	
	/**
	 * @return Name of the property that contains item deprecation time
	 */
	def deprecationAttName: String
	
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
	 * @return Column that contains item deprecation timestamp
	 */
	def deprecationColumn = table(deprecationAttName)
	
	/**
	 * @return A model that has just been marked as deprecated
	 */
	def nowDeprecated = withDeprecatedAfter(Now)
	
	/**
	 * @return A deletion rule that deletes deprecated items as soon as possible
	 */
	def immediateDeletionRule = DataDeletionRule.onArrivalOf(table, deprecationAttName)
	
	
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
	
	/**
	 * @param historyDuration Duration how long the item is kept in the database after deprecation
	 * @return A new deletion rule that applies to this model type
	 */
	def deletionAfterDeprecation(historyDuration: FiniteDuration) =
		DataDeletionRule(table, deprecationAttName, historyDuration)
}
