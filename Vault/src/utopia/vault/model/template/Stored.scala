package utopia.vault.model.template

import utopia.flow.view.template.Extender

/**
  * A common trait for models that combine a data portion with a table row id. Represents data after it has been
  * inserted to the database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1.6.1
  */
trait Stored[+Data, +Id] extends Extender[Data] with HasId[Id]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The data wrapped by this stored instance
	  */
	def data: Data
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped = data
}
