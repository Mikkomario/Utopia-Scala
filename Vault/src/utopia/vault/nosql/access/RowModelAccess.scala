package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromRowFactory
import utopia.vault.sql.Condition

/**
  * An access point for models that can be constructed from single row's data
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  * @tparam M Type of model returned
  * @tparam A Format in which the model/models are returned (Eg. option or vector)
  */
trait RowModelAccess[+M, +A] extends ModelAccess[M, A]
{
	// ABSTRACT	-------------------------
	
	override def factory: FromRowFactory[M]
	
	
	// OTHER	-------------------------
	
	/**
	  * Performs an operation for each of the items accessible from this accessor
	  * @param f Function performed for each targeted item
	  * @param connection DB Connection (implicit)
	  * @tparam U Arbitrary result type
	  */
	def foreach[U](f: M => U)(implicit connection: Connection) = factory.foreachWhere(globalCondition)(f)
	
	/**
	  * Performs an operation over a subset of items accessible from this accessor
	  * @param additionalCondition Additional targeting condition
	  * @param f Function performed for each targeted item
	  * @param connection DB Connection (implicit)
	  * @tparam U Arbitrary result type
	  */
	def foreachWhere[U](additionalCondition: Condition)(f: M => U)(implicit connection: Connection) =
		factory.foreachWhere(mergeCondition(additionalCondition))(f)
}
