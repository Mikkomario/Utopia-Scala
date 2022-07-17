package utopia.vault.coder.model.enumeration

import utopia.vault.coder.model.scala.template.{ScalaTypeConvertible, ValueTypeConvertible}

/**
  * A common trait for property type implementations that don't account for null / None values, ie. the "concrete"
  * type variants.
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.15.1
  */
trait BasePropertyType extends ScalaTypeConvertible with ValueTypeConvertible
{
	/**
	  * @return Converts this property type into SQL. Doesn't include any NOT NULL or DEFAULT -statements.
	  */
	def toBaseSql: String
}
