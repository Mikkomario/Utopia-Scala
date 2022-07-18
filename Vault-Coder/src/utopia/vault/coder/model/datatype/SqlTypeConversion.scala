package utopia.vault.coder.model.datatype

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.ScalaType
import utopia.vault.coder.model.scala.template.ValueConvertibleType

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
	
	
	// NESTED   ---------------------
	
	private class _SqlTypeConversion(override val origin: ScalaType, override val intermediate: ValueConvertibleType,
	                                 override val target: SqlPropertyType)
	                                (mid: String => CodePiece)
		extends SqlTypeConversion
	{
		override def midConversion(originCode: String) = mid(originCode)
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
	                 indexByDefault: Boolean = target.indexByDefault) = SqlTypeConversion(origin, intermediate,
		target.copy(defaultValue = defaultValue, columnNameSuffix = columnNameSuffix, indexByDefault = indexByDefault))(
		midConversion)
}
