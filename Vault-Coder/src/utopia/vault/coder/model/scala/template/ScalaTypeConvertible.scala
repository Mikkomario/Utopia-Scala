package utopia.vault.coder.model.scala.template

import utopia.vault.coder.model.scala.datatype.ScalaType

/**
  * A common trait for items which may be converted to Scala data types
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
trait ScalaTypeConvertible
{
	/**
	  * @return A Scala representation of this data type
	  */
	def toScala: ScalaType
}
