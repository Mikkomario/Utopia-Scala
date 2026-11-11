package utopia.flow.generic.operation

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.generic.model.enumeration.ValueOperation1.Negate
import utopia.flow.generic.model.enumeration.ValueOperation2.{Minus, Plus}
import utopia.flow.generic.model.enumeration.{ValueOperation1, ValueOperation2}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType

import scala.annotation.tailrec

/**
 * An interface for applying operators to values
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
object ValueOperator
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Contains supported unary operation implementations
	 */
	private var operators1: Seq[ValueOperator1] = Single(BasicValueOperator1)
	/**
	 * Contains supported binary operation implementations
	 */
	private var operators2: Seq[ValueOperator2] = Single(BasicValueOperator2)
	
	/**
	 * Caches supported unary operations.
	 *      - Primary keys are operations
	 *      - Secondary keys are input types
	 *      - Values are operators able to handle that operation
	 */
	private val supportedOptions1Cache = Cache.clearable { operation: ValueOperation1 =>
		operators1.iterator.flatMap { o => o.supportedTypes.iterator.filter { _._2 == operation }.map { _._1 -> o } }
			.toMap
	}
	/**
	 * Caches supported binary operations.
	 *      - Primary keys are operations
	 *      - Secondary keys are primary input types
	 *      - Tertiary keys are secondary input types
	 *      - Values are operators able to handle that combination
	 */
	private val supportedOptions2Cache = Cache.clearable { operation: ValueOperation2 =>
		operators2.iterator
			.flatMap { o =>
				o.supportedOperations.iterator.filter { _._2 == operation }
					.flatMap { case (primaryType, _, secondaryTypes) =>
						secondaryTypes.iterator.map { secondaryType => primaryType -> (secondaryType, o) }
					}
			}
			.groupMapMap { _._1 } { _._2 } { _.toMap }
	}
	
	
	// OTHER    ---------------------------
	
	/**
	 * Adds a new implementation for handling (unary) value operations
	 * @param operator An operator implementation
	 */
	def registerOperator1(operator: ValueOperator1) = {
		if (!operators1.contains(operator)) {
			operators1 :+= operator
			supportedOptions1Cache.clear()
		}
	}
	/**
	 * Adds a new implementation for handling (binary) value operations
	 * @param operator An operator implementation
	 */
	def registerOperator2(operator: ValueOperator2) = {
		if (!operators2.contains(operator)) {
			operators2 :+= operator
			supportedOptions2Cache.clear()
		}
	}
	
	/**
	 * Executes a unary value operation
	 * @param value Value to operate on
	 * @param operation Operation to apply
	 * @return Operation result
	 */
	@throws[UnsupportedOperationException]("If the specified operation is not supported for the specified value")
	def apply(value: Value, operation: ValueOperation1) = {
		// Case: Empty value => Not modified by any operation
		if (value.isEmpty)
			value
		else {
			// Attempts to cast the value to one of the supported types
			val supportedOptions = supportedOptions1Cache(operation)
			value.castTo(supportedOptions.keys) match {
				// Case: Casting succeeded => Applies the operation
				case Some(value) => supportedOptions(value.dataType).apply(value, operation)
				// Case: Not possible to cast => Throws
				case None => throw new UnsupportedOperationException(s"$operation is not supported for $value")
			}
		}
	}
	/**
	 * Executes a binary value operation
	 * @param primary The primary value
	 * @param operation Operation to apply
	 * @param secondary The secondary value
	 * @throws scala.UnsupportedOperationException If the specified operation is not supported for the specified values
	 * @return Operation result
	 */
	@throws[UnsupportedOperationException]("If the specified operation is not supported for the specified values")
	def apply(primary: Value, operation: ValueOperation2, secondary: Value): Value = {
		// Case: Secondary parameter is empty => Preserves the primary value as is
		if (secondary.isEmpty)
			primary
		// Case: Primary value is empty
		//       => Converts the operation into a unary operation and applies it to the secondary value
		else if (primary.isEmpty)
			operation match {
				// Case: Plus => Returns the secondary value only
				case Plus => secondary
				// Case: Minus => Negates and returns the secondary value
				case Minus => apply(secondary, Negate)
			}
		// Case: Both values are non-empty => Casts the primary value to one of the supported types
		else
			_apply(primary, operation, secondary, supportedOptions2Cache(operation))
	}
	
	@tailrec
	private def _apply(primary: Value, operation: ValueOperation2, secondary: Value,
	                   remainingSupport: Map[DataType, Map[DataType, ValueOperator2]]): Value =
		primary.castTo(remainingSupport.keys) match {
			// Case: Casting succeeded => Casts the secondary value to one of the supported types
			case Some(primary) =>
				val secondaryOptions = remainingSupport(primary.dataType)
				secondary.castTo(secondaryOptions.keys) match {
					// Case: Casting succeeded => Applies the operation
					case Some(secondary) =>
						secondaryOptions(secondary.dataType).apply(primary, operation, secondary)
					// Case: Not possible to cast => Attempts with another primary type, if possible
					case None => _apply(primary, operation, secondary, remainingSupport - primary.dataType)
				}
			// Case: Not possible to cast => Throws
			case None =>
				throw new UnsupportedOperationException(s"Unsupported operation $primary $operation $secondary")
		}
}
