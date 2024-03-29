package utopia.metropolis.model.stored

import utopia.flow.view.template.Extender

/**
  * A common trait for data that has been stored to database
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
trait Stored[+Data] extends Extender[Data]
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return This stored instance's row id
	  */
	def id: Int
	
	/**
	  * @return Data contained within this instance
	  */
	def data: Data
	
	
	// IMPLEMENTED	-----------------------------
	
	override def wrapped = data
}
