package utopia.vault.test.app

import utopia.flow.generic.model.mutable.DataType.{BooleanType, IntType, StringType}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.StringExtensions._
import utopia.vault.model.immutable.{Column, Storable, Table}

/**
 *
 * @author Mikko Hilpinen
 * @since 28.10.2025, v
 */
object ModelToConditionTest extends App
{
	private val table = Table("test", "test",
		Vector(Column("a", "a", "test", IntType), Column("b", "b", "test", StringType),
			Column("c", "c", "test", BooleanType, defaultValue = true)))
	
	private val dbModel = new DbModel(a = Some(1))
	private val model = dbModel.toModel
	private val condition = dbModel.toCondition
	println(model)
	println(model.properties)
	println(condition)
	assert(model("c").isEmpty)
	assert(!condition.toString.containsIgnoreCase("AND"))
	
	private class DbModel(a: Option[Int] = None, b: String = "", c: Option[Boolean] = None) extends Storable
	{
		override val table: Table = ModelToConditionTest.table
		override def valueProperties: Iterable[(String, Value)] = Vector("a" -> a, "b" -> b, "c" -> c)
	}
}
