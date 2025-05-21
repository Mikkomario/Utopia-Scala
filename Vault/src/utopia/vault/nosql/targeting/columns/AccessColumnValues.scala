package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessManyColumns

object AccessColumnValues
{
	// OTHER    --------------------------
	
	/**
	  * Creates an access point to an individual column's values
	  * @param access Access point used for accessing column values
	  * @param column Targeted column
	  * @return A new access point
	  */
	def apply(access: AccessManyColumns, column: Column) = new AccessColumnValuesFactory(access, column)
		
	
	// NESTED   -------------------------
	
	class AccessColumnValuesFactory(access: AccessManyColumns, column: Column)
		extends ColumnValueAccessFactory[AccessColumnValues]
	{
		override def customInput[A, I](parse: Value => A)(toValue: I => Value) =
			new AccessColumnValues[A, I](access, column)(parse)(toValue)
	}
}

/**
  * An interface that provides access to a single column's values
  * @tparam A Type of parsed column value
  * @tparam In Type of accepted input when assigning values
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
class AccessColumnValues[+A, -In](override protected val access: AccessManyColumns, override val column: Column)
                                 (f: Value => A)(implicit toValue: In => Value)
	extends ColumnValueAccess[Seq[Value], Seq[A], In]
{
	// COMPUTED ------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return Distinct accessible values of this column
	  */
	def distinct(implicit connection: Connection) = access(column, distinct = true).map(f)
	/**
	  * @param connection Implicit DB connection
	  * @return Values of this column mapped to the primary index of the matching row
	  */
	def byId(implicit connection: Connection) =
		access(access.index, column).iterator.map { vals => vals.head.getInt -> f(vals(1)) }.toMap
	
	
	// IMPLEMENTED  --------------
	
	override protected def parse(value: Seq[Value]): Seq[A] = value.map(f)
	override protected def valueOf(value: In): Value = toValue(value)
}
