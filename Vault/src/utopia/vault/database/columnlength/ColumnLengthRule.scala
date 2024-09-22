package utopia.vault.database.columnlength

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.util.TryExtensions._
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRule.CombiningRule
import utopia.vault.model.error.MaxLengthExceededException
import utopia.vault.model.immutable.Column

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Rules applied to situations where column maximum length is exceeded
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnLengthRule
{
	// ABSTRACT ---------------------------------
	
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
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param backupRule A rule to use if this rule throws
	  * @return A copy of this rule that uses the second rule in case this one fails
	  */
	def recoverWith(backupRule: ColumnLengthRule): ColumnLengthRule = new CombiningRule(this, backupRule)
}

object ColumnLengthRule
{
	/**
	  * A rule which throws an error whenever column maximum length is exceeded
	  */
	object Throw extends ColumnLengthRule
	{
		override def toString = "throw"
		
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
			lengthLimit.test(proposedValue).toOption
				.getOrElse { throw new MaxLengthExceededException(
					s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
	}
	
	/**
	  * A rule which attempts to crop the input value to fit the maximum column length
	  */
	object TryCrop extends ColumnLengthRule
	{
		override def toString = "crop"
		
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
		{
			lengthLimit.test(proposedValue, allowCrop = true).toOption
				.getOrElse { throw new MaxLengthExceededException(
					s"${column.columnNameWithTable}'s maximum length is exceeded by: $proposedValue") }
		}
	}
	
	object TryExpand
	{
		/**
		  * @param exc Implicit execution context
		  * @param connectionPool Implicit connection pool
		  * @return A rule which expands the applicable column as far as possible, if necessary
		  */
		def infinitely(implicit exc: ExecutionContext, connectionPool: ConnectionPool) = TryExpand()
		/**
		  * @param limit Maximum expanded length allowed
		  * @param exc Implicit execution context
		  * @param connectionPool Implicit connection pool
		  * @return A rule which expands the applicable column up to a certain maximum threshold
		  */
		def upTo(limit: Long)(implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
			TryExpand(Some(limit))
	}
	
	/**
	  * A rule which attempts to expand the column maximum limit to fit the new value
	  * @param exc Implicit Execution context
	  * @param connectionPool Implicit Connection pool
	  */
	case class TryExpand(maximum: Option[Long] = None)
	                    (implicit exc: ExecutionContext, connectionPool: ConnectionPool)
		extends ColumnLengthRule
	{
		override def toString = maximum match {
			case Some(max) => s"expand up to $max"
			case None => "expand indefinitely"
		}
		
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
		{
			lengthLimit.test(proposedValue) match {
				case Right(value) => value
				case Left(largerLimit) =>
					largerLimit match {
						case Some(largerLimit) =>
							// Checks whether the larger limit meets the maximum defined in this rule
							// Case: New limit is OK
							if (maximum.forall { _ >= largerLimit.maxValue }) {
								// Applies the larger length limit immediately
								val nullStr = if (column.allowsNull) "" else " NOT NULL"
								val incrementStr = if (column.usesAutoIncrement) " AUTO_INCREMENT" else ""
								val defaultStr = column.defaultValue.notEmpty match {
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
										s"ALTER TABLE `${ column.tableName }` MODIFY `${ column.columnName }` ${
											largerLimit.sqlType
										}$nullStr$incrementStr$defaultStr")
								}
								// Remembers the extended length limit
								ColumnLengthLimits((databaseName, column.tableName, column.name)) = largerLimit
								proposedValue
							}
							// Case: Required limit is too high => fails
							else
								throw new MaxLengthExceededException(
									s"${ column.columnNameWithTable } can't be extended to required length")
						case None => throw new MaxLengthExceededException(
							s"${ column.columnNameWithTable } can't fit $proposedValue and can't be expanded")
					}
			}
		}
	}
	
	// Combines two length rules, uses the second one if the first one throws
	private class CombiningRule(primary: ColumnLengthRule, secondary: ColumnLengthRule) extends ColumnLengthRule
	{
		override def toString = s"$primary or $secondary"
		
		override def apply(databaseName: String, column: Column, lengthLimit: ColumnLengthLimit, proposedValue: Value) =
		{
			Try { primary(databaseName, column, lengthLimit, proposedValue) }.getOrMap { error =>
				Try { secondary(databaseName, column, lengthLimit, proposedValue) }.getOrElse { throw error }
			}
		}
	}
}
