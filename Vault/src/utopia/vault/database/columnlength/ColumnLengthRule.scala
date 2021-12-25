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
	  * @param column Column whose value is being updated or inserted
	  * @param proposedValue The proposed value
	  * @return Value to insert / update - May be modified or may be the same
	  * @throws Exception May throw an exception if the value is not accepted
	  */
	def apply(column: Column, proposedValue: Value): Value
}

object ColumnLengthRule
{
	/**
	  * A rule which throws an error whenever column maximum length is exceeded
	  */
	object Throw extends ColumnLengthRule
	{
		override def apply(column: Column, proposedValue: Value) =
			column.lengthLimit match {
				case Some(limit) =>
					limit.test(proposedValue).toOption
						.getOrElse { throw new MaxLengthExceededException(
							s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
				case None => proposedValue
			}
	}
	
	/**
	  * A rule which attempts to crop the input value to fit the mximum column length
	  */
	object TryCrop extends ColumnLengthRule
	{
		override def apply(column: Column, proposedValue: Value) = column.lengthLimit match {
			case Some(limit) =>
				limit.test(proposedValue, allowCrop = true).toOption
					.getOrElse { throw new MaxLengthExceededException(
						s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
			case None => proposedValue
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
		override def apply(column: Column, proposedValue: Value) = column.lengthLimit match
		{
			case Some(limit) =>
				limit.test(proposedValue) match {
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
								// TODO: Should affect the current table column
								connectionPool { _.execute(
									s"ALTER TABLE ${column.tableName} MODIFY ${column.columnName} ${
										largerLimit.sqlType}$nullStr$incrementStr$defaultStr") }
								proposedValue
								
							case None => throw new MaxLengthExceededException(
								s"${column.columnNameWithTable} can't fit $proposedValue and can't be expanded")
						}
				}
			case None => proposedValue
		}
	}
}
