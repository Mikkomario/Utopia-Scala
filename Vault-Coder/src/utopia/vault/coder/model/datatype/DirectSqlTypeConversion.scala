package utopia.vault.coder.model.datatype

import utopia.vault.coder.model.scala.template.ValueConvertibleType

/**
  * A common trait for SQL type conversions that don't need an intermediate step because the original type already
  * accepts a None or other empty value and is convertible to a Value by itself
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
case class DirectSqlTypeConversion(scalaType: ValueConvertibleType, sqlType: SqlPropertyType) extends SqlTypeConversion
{
	override def origin = scalaType.scalaType
	override def intermediate = scalaType
	override def target = sqlType
	
	override def midConversion(originCode: String) = originCode
}
