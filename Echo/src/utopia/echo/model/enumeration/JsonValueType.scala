package utopia.echo.model.enumeration

/**
  * An enumeration which represents the standard json value types understood and produced by LLM interfaces
  * in the context of generating json.
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
sealed trait JsonValueType
{
	// ABSTRACT --------------------
	
	def name: String
	def pluralName: String
	
	
	// IMPLEMENTED  ----------------
	
	override def toString = name
}

object JsonValueType
{
	// VALUES   --------------------------
	
	case object AnyType extends JsonValueType
	{
		override def name: String = ""
		override def pluralName: String = ""
	}
	
	case object StringValue extends JsonValueType
	{
		override def name: String = "string"
		override def pluralName: String = "strings"
	}
	
	case object IntValue extends JsonValueType
	{
		override def name: String = "integer"
		override def pluralName: String = "integers"
	}
	
	case object NumberValue extends JsonValueType
	{
		override def name: String = "number"
		override def pluralName: String = "numbers"
	}
	
	case class ValuesArray(innerType: JsonValueType) extends JsonValueType
	{
		override def name: String = innerType match {
			case AnyType => "array"
			case t => s"array of ${ t.pluralName }"
		}
		override def pluralName: String = innerType match {
			case AnyType => "arrays"
			case at: ValuesArray => s"arrays of ${ at.pluralName }"
			case t => s"${ t.name } arrays"
		}
	}
}