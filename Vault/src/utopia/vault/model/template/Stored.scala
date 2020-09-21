package utopia.vault.model.template

import scala.language.implicitConversions

object Stored
{
	/**
	  * Implicitly accesses the data element of the specified stored instance
	  * @param s A stored instance
	  * @tparam Data Type of data within that instance
	  * @return Data element inside that stored instance
	  */
	implicit def autoUnwrap[Data](s: Stored[Data, _]): Data = s.data
}

/**
  * A common trait for models that combine a data portion with a table row id. Represents data after it has been
  * inserted to the database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1.6.1
  */
trait Stored[+Data, +Id]
{
	/**
	  * @return The table row id associated with this stored instance
	  */
	def id: Id
	
	/**
	  * @return The data wrapped by this stored instance
	  */
	def data: Data
}
