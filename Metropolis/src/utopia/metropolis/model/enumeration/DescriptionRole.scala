package utopia.metropolis.model.enumeration

import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.NoSuchTypeException

/**
  * A common trait for description role values
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
sealed trait DescriptionRole
{
	/**
	  * @return Id of this role
	  */
	def id: Int
	
	/**
	  * @return Key that describes this description role in json context
	  */
	def jsonKey: String
	
	/**
	  * @return Key that describes this description role in json context, when there are multiple values described
	  */
	def jsonKeyPlural: String
}

object DescriptionRole
{
	/**
	  * Name descriptions simply name various resources
	  */
	case object Name extends DescriptionRole
	{
		override def id = 1
		
		override def jsonKey = "name"
		
		override def jsonKeyPlural = "names"
	}
	
	/**
	  * All description role values recorded in code
	  */
	val values = Vector[DescriptionRole](Name)
	
	/**
	  * @param id Description role id
	  * @return a role matching specified id
	  */
	def forId(id: Int) = values.find { _.id == id }.toTry {
		new NoSuchTypeException(s"No description role with id $id") }
}
