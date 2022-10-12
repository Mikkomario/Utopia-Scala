package utopia.vault.coder.test

import utopia.flow.generic.model.mutable.DataType
import utopia.vault.coder.model.data.{Enum, EnumerationValue, NamingRules}
import utopia.vault.coder.model.datatype.PropertyType
import utopia.vault.coder.model.datatype.PropertyType.{EnumValue, NonEmptyText, Text}
import utopia.vault.coder.model.scala.Package

/**
  * Tests data types
  * @author Mikko Hilpinen
  * @since 11.8.2022, v1.6
  */
object TypeTest extends App
{
	implicit val naming: NamingRules = NamingRules.default
	
	val t = PropertyType.interpret("Option[String]", Some(2), Some("isoCode")).get
	println(t)
	assert(t == Text(2))
	
	val t2 = PropertyType.interpret("NonEmptyString(5)").get
	println(t2)
	assert(t2 == NonEmptyText(5))
	
	val v = EnumerationValue("Test1", "1")
	val e = Enum("Test", Package.vault, Vector(v), Some(v))
	val et = EnumValue(e)
	
	println(et.defaultValue.text)
	println(et.defaultValue.references.mkString(", "))
	
	println("Success")
}
