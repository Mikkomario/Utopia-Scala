package utopia.metropolis.model.enumeration

import utopia.flow.view.template.Extender

/**
  * A common trait for database-originated enumeration value wrappers (code references to individual known ids)
  * @author Mikko Hilpinen
  * @since 21.2.2022, v2.1
  */
trait IdWrapper extends Extender[Int]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The wrapped id
	  */
	def id: Int
	
	
	// IMPLEMENTED  ---------------------
	
	override def wrapped = id
}
