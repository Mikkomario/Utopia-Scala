package utopia.vault.coder.test

import utopia.flow.generic.DataType
import utopia.vault.coder.model.datatype.PropertyType
import utopia.vault.coder.model.datatype.PropertyType.{NonEmptyText, Text}

/**
  * Tests data types
  * @author Mikko Hilpinen
  * @since 11.8.2022, v1.6
  */
object TypeTest extends App
{
	DataType.setup()
	
	val t = PropertyType.interpret("Option[String]", Some(2), Some("isoCode")).get
	println(t)
	assert(t == Text(2))
	
	val t2 = PropertyType.interpret("NonEmptyString(5)").get
	println(t2)
	assert(t2 == NonEmptyText(5))
	
	println("Success")
}
