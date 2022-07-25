package utopia.flow.test.file

import utopia.flow.async.ThreadPool
import utopia.flow.container.ObjectMapFileContainer
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.{DataType, FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
  * Tests container implementations
  * @author Mikko Hilpinen
  * @since 5.7.2022, v1.16
  */
object ContainerTest extends App
{
	DataType.setup()
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("test").executionContext
	implicit val jsonParser: JsonParser = JSONReader
	
	val container = new ObjectMapFileContainer("Flow/test/test-container.json", TestObject)
	
	container("test") = TestObject("test", 1)
	
	object TestObject extends FromModelFactoryWithSchema[TestObject]
	{
		override val schema = ModelDeclaration("name" -> StringType)
		
		override protected def fromValidatedModel(model: Model) =
			apply(model("name").getString, model("value").getInt)
	}
	case class TestObject(name: String, value: Int) extends ModelConvertible
	{
		override def toModel = Model(Vector("name" -> name, "value" -> value))
	}
}
