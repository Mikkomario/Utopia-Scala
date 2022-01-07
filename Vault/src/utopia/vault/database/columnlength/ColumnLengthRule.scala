package utopia.vault.database.columnlength

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.StringType
import utopia.vault.database.ConnectionPool
import utopia.vault.model.error.MaxLengthExceededException
import utopia.vault.model.immutable.Column

import scala.concurrent.ExecutionContext

/**
  * Rules applied to situations where column maximum length is exceeded
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnLengthRule
{
	/**
	  * Tests a case where updating or inserting a column value.
	  * Testing doesn't have to be limited to length-exceeding cases.
	  * @param databaseName Name of the targeted database
	  * @param column Column whose value is being updated or inserted
	  * @param lengthLimit Length limit to apply to that column
	  * @param proposedValue The proposed value
	  * @return Value to insert / update - May be modified or may be the same
	  * @throws Exception May throw an exception if the value is not accepted
	  */
	def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value): Value
}

object ColumnLengthRule
{
	/**
	  * A rule which throws an error whenever column maximum length is exceeded
	  */
	object Throw extends ColumnLengthRule
	{
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
			lengthLimit.test(proposedValue).toOption
				.getOrElse { throw new MaxLengthExceededException(
					s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
	}
	
	/**
	  * A rule which attempts to crop the input value to fit the mximum column length
	  */
	object TryCrop extends ColumnLengthRule
	{
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
		{
			lengthLimit.test(proposedValue, allowCrop = true).toOption
				.getOrElse { throw new MaxLengthExceededException(
					s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
		}
	}
	
	/**
	  * A rule which attempts to expand the column maximum limit to fit the new value
	  * @param exc Implicit Execution context
	  * @param connectionPool Implicit Connection pool
	  */
	// TODO: Add maximum expand limit support
	case class TryExpand()(implicit exc: ExecutionContext, connectionPool: ConnectionPool) extends ColumnLengthRule
	{
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
		{
			lengthLimit.test(proposedValue) match {
				case Right(value) => value
				case Left(largerLimit) =>
					largerLimit match {
						case Some(largerLimit) =>
							// Applies the larger length limit immediately
							val nullStr = if (column.allowsNull) "" else " NOT NULL"
							val incrementStr = if (column.usesAutoIncrement) " AUTO_INCREMENT" else ""
							val defaultStr = column.defaultValue match {
								case Some(defaultValue) =>
									defaultValue.dataType match {
										case StringType => s" '$defaultValue'"
										case _ => defaultValue.toString
									}
								case None => ""
							}
							connectionPool { implicit connection =>
								connection.dbName = databaseName
								connection.execute(
									s"ALTER TABLE ${ column.tableName } MODIFY ${ column.columnName } ${
										largerLimit.sqlType
									}$nullStr$incrementStr$defaultStr")
							}
							// Remembers the extended length limit
							ColumnLengthLimits((databaseName, column.tableName, column.name)) = largerLimit
							proposedValue
						
						case None => throw new MaxLengthExceededException(
							s"${ column.columnNameWithTable } can't fit $proposedValue and can't be expanded")
					}
			}
		}
	}
}
