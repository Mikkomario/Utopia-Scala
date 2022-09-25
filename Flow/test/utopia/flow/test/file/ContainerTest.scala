package utopia.flow.test.file

import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.file.container.{ObjectMapFileContainer, ValueConvertibleFileContainer}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.{DataType, StringType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.parse.json.{JSONReader, JsonParser}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import java.time.Instant
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
	
	val container2 = new ValueConvertibleFileContainer[Option[Instant]]("Flow/test/test-container-2.json")
	
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
