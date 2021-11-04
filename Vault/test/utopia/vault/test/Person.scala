package utopia.vault.test

import java.time.Instant
import utopia.flow.datastructure.immutable
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

object Person extends FromValidatedRowModelFactory[Person]
{
    // ATTRIBUTES    ----------------
    
    override val table = TestTables.person
    
    
    // IMPLEMENTED METHODS    -------
    
    override protected def fromValidatedModel(model: immutable.Model) = new Person(model("name").getString,
        model("age").int, model("isAdmin").getBoolean, model("created").getInstant, model("rowId").int)
}

/**
 * This is a test model class that implements the storable trait
 * @author Mikko Hilpinen
 * @since 18.6.2017
 */
case class Person(name: String, age: Option[Int] = None, isAdmin: Boolean = false,
        created: Instant = Now, rowId: Option[Int] = None) extends StorableWithFactory[Person]
{
    // COMPUTED PROPERTIES    ------------------
    
    override def factory = Person
    
    override def valueProperties = Vector("name" -> name, "age" -> age, "isAdmin" -> isAdmin, 
            "created" -> created, "rowId" -> rowId)
}