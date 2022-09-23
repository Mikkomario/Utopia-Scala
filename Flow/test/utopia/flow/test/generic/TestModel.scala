package utopia.flow.test.generic

import utopia.flow.collection.template.typeless
import utopia.flow.collection.template.typeless.Property
import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.template
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.operator.Equatable

import scala.util.{Failure, Success}

object TestModel extends FromModelFactory[TestModel]
{
    def apply(model: typeless.Model[Property]) =
    {
        val name = model("name").string
        
        // Name field is required
        if (name.isDefined)
        {
            val level = model("level").intOr(1)
            val stats = model("stats").modelOr().toMap { _.int }
            val title = model("title").string
            
            Success(new TestModel(name.get, level, stats, title))
        }
        else
            Failure(new NoSuchElementException(s"Cannot parse TestModel from $model without 'name' property"))
    }
}

/**
 * This is a test implementation of ModelConvertible / FromModelParseable traits
 * @author Mikko Hilpinen
 * @since 24.6.2017
 */
class TestModel(val name: String, val level: Int = 1, val stats: Map[String, Int], 
        val title: Option[String] = None) extends ModelConvertible with Equatable
{
    // COMPUTED PROPERTIES    ------------------
    
    override def toModel = Model(Vector("name" -> name, "level" -> level, 
            "stats" -> Model.fromMap(stats), "title" -> title))
    
    override def properties = Vector(name, level, stats, title)
}