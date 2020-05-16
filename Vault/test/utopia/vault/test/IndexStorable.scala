package utopia.vault.test

import utopia.flow.datastructure.immutable
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.factory.FromValidatedRowModelFactory

object IndexStorable extends FromValidatedRowModelFactory[IndexStorable]
{
	override def table = TestTables.indexTest
	
	override protected def fromValidatedModel(model: immutable.Model[Constant]) = IndexStorable(model("id").int, model("text").string)
	
	def apply(id: Int, text: String): IndexStorable = IndexStorable(Some(id), Some(text))
}

/**
  * Simple storable for index_test table
  * @author Mikko Hilpinen
  * @since 12.7.2019, v1.2.2+
  */
case class IndexStorable(id: Option[Int] = None, text: Option[String] = None) extends StorableWithFactory[IndexStorable]
{
	override def factory = IndexStorable
	
	override def valueProperties = Vector("id" -> id, "text" -> text)
}
