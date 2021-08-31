package utopia.citadel.coder.model.scala

/**
  * Represents a scala method parameter
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Parameter(name: String, dataType: ScalaType, default: String = "", prefix: String = "")
	extends Referencing with ScalaConvertible
{
	// IMPLEMENTED  ---------------------------
	
	def references = dataType.references.toSet
	
	override def toScala = {
		val prefixString = if (prefix.isEmpty) "" else s"$prefix "
		val defaultString = if (default.isEmpty) "" else s" = $default"
		s"$prefixString$name: ${dataType.toScala}$defaultString"
	}
}
