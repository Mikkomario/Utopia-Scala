package utopia.scribe.core.model.stored

import utopia.flow.view.template.Extender

/**
  * Common trait for classes that are stored in the database,
  * wrapping a separate instance and combining it with a database row id
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  */
trait Stored[+Data] extends Extender[Data]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return Id of this item in the database
	  */
	def id: Int
	/**
	  * @return Wrapped data
	  */
	def data: Data
	
	
	// IMPLEMENTED  -------------------
	
	override def wrapped: Data = data
}
