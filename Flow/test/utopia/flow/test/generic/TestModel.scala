package utopia.flow.test.generic

import utopia.flow.generic.model.template
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.operator.equality.EqualsBy

import scala.util.{Failure, Success}

object TestModel extends FromModelFactory[TestModel]
{
    def apply(model: template.ModelLike[Property]) =
    {
        val name = model("name").string
        
        // Name field is required
        if (name.isDefined)
        {
            val level = model("level").intOr(1)
            val stats = model("stats").getModel.toPartialMap { _.int }
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
        val title: Option[String] = None) extends ModelConvertible with EqualsBy
{
    // COMPUTED PROPERTIES    ------------------
    
    override def toModel = Model(Vector("name" -> name, "level" -> level, 
            "stats" -> Model.fromMap(stats), "title" -> title))
    
    protected override def equalsProperties: Seq[Any] = Vector(name, level, stats, title)
}