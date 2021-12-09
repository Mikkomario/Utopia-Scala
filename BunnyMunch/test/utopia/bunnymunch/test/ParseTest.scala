package utopia.bunnymunch.test

import org.typelevel.jawn.Parser
import utopia.bunnymunch.jawn.ValueFacade
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._

import scala.util.{Failure, Success}

/**
  * Tests JSON parsing using Jawn and ValueFacade
  * @author Mikko Hilpinen
  * @since 12.5.2020, v1
  */
object ParseTest extends App
{
	DataType.setup()
	implicit val facade: ValueFacade.type = ValueFacade
	
	val subModel = Model(Vector("name" -> "Öykkäri", "age" -> 44, "length" -> 187.2))
	val model = Model(Vector("tester" -> subModel,
		"test_description" -> "Testing \"BunnyMunch\"\nSecond line", "empty" -> Value.empty,
		"json" -> "{[1, 2, 3]}"))
	val json = model.toJson
	println(json)
	
	Parser.parseFromString(json) match
	{
		case Success(value) =>
			println(s"Successfully parsed a value:")
			println(value.getString)
			println(model.getString)
			assert(value.getModel == model)
		case Failure(error) => error.printStackTrace()
	}
	
	assert(Parser.parseFromString("3.21").get.getDouble == 3.21)
}
