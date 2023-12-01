package utopia.vault.coder.model.datatype

import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.ScalaType
import utopia.coder.model.scala.template.ValueConvertibleType

object SqlTypeConversion
{
	// OTHER    ---------------------
	
	/**
	  * Creates a new type conversion
	  * @param origin The origin data type (Scala)
	  * @param intermediate The intermediate data type (Scala)
	  * @param target The target data type (SQL)
	  * @param midConversion Conversion from origin to intermediate. Accepts reference, returns code.
	  * @return A new SQL type conversion
	  */
	def apply(origin: ScalaType, intermediate: ValueConvertibleType, target: SqlPropertyType)
		(midConversion: String => CodePiece): SqlTypeConversion =
		new _SqlTypeConversion(origin, intermediate, target)(midConversion)
	
	/**
	  * Creates a new type conversion that utilizes/wraps another type conversion
	  * @param lower The type conversion that handles the mid-conversion & sql type representation
	  * @param fromType New origin scala type
	  * @param convertToLower A function that accepts a reference to the 'fromType' scala type value and
	  *                       returns an instance accepted by the 'lower' sql conversion.
	  *                       E.g. If the upper type is convertible to a Double,
	  *                       this function would perform that conversion.
	  * @return A new SQL type conversion that delegates most functionality to another conversion instance,
	  *         providing a wrapping interface.
	  */
	def delegatingTo(lower: SqlTypeConversion, fromType: ScalaType)
	                (convertToLower: String => CodePiece): SqlTypeConversion =
		new DelegatingTypeConversion(fromType, lower, convertToLower)
	
	
	// NESTED   ---------------------
	
	private class _SqlTypeConversion(override val origin: ScalaType, override val intermediate: ValueConvertibleType,
	                                 override val target: SqlPropertyType)
	                                (mid: String => CodePiece)
		extends SqlTypeConversion
	{
		override def midConversion(originCode: String) = mid(originCode)
	}
	private class DelegatingTypeConversion(override val origin: ScalaType, delegate: SqlTypeConversion,
	                                       conversionCode: String => CodePiece)
		extends SqlTypeConversion
	{
		override def intermediate: ValueConvertibleType = delegate.intermediate
		override def target: SqlPropertyType = delegate.target
		
		override def midConversion(originCode: String): CodePiece = {
			val converted = conversionCode(originCode)
			delegate.midConversion(converted.text).referringTo(converted.references)
		}
	}
}

/**
  * Represents a conversion from a single scala type to a single SQL type
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
trait SqlTypeConversion
{
	// ABSTRACT ---------------------
	
	/**
	  * @return The original Scala data type that starts the conversion
	  */
	def origin: ScalaType
	/**
	  * @return The intermediate, value-convertible Scala type used in mid-conversion
	  */
	def intermediate: ValueConvertibleType
	/**
	  * @return The resulting sql type after background value conversions
	  */
	def target: SqlPropertyType
	
	/**
	  * Writes code that converts an instance from the origin type to the intermediate type
	  * @param originCode Code that refers to an instance of the original data type
	  * @return Code that returns an instance of the intermediate data type.
	  */
	def midConversion(originCode: String): CodePiece
	
	
	// COMPUTED -----------------
	
	/**
	  * @return Whether no intermediate conversion is needed because the origin and intermediate types are the same
	  */
	def isDirect = origin == intermediate.scalaType
	/**
	  * @return Whether an intermediate conversion is needed because the origin and intermediate types differ
	  */
	def isIndirect = !isDirect
	
	
	// OTHER    -----------------
	
	/**
	  * Modifies the target SQL type without affecting conversion logic
	  * @param defaultValue New default value (default = previous)
	  * @param columnNameSuffix New column name suffix (default = previous)
	  * @param indexByDefault Whether index should be created by default (default = previous)
	  * @return A modified version of this conversion
	  */
	def modifyTarget(defaultValue: String = target.defaultValue, columnNameSuffix: String = target.columnNameSuffix,
	                 indexByDefault: Boolean = target.indexByDefault) =
		SqlTypeConversion(origin, intermediate,
			target.copy(defaultValue = defaultValue, columnNameSuffix = columnNameSuffix,
				indexByDefault = indexByDefault))(midConversion)
}
