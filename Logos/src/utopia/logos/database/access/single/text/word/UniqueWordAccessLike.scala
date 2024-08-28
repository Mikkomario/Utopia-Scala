package utopia.logos.database.access.single.text.word

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.storable.text.WordDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target individual words or similar items at a time
  * @tparam A Type of read (words -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueWordAccessLike[+A, +Repr] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with FilterableView[Repr] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Text representation of this word. 
	  * None if no word (or value) was found.
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).getString
	/**
	  * Time when this word was added to the database. 
	  * None if no word (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	/**
	  * Unique id of the accessible word. None if no word was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = WordDbModel
}

