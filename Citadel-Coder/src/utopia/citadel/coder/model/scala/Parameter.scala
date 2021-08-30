package utopia.citadel.coder.model.scala

/**
  * Represents a scala method parameter
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Parameter(name: String, dataType: ScalaType, default: String = "") extends Referencing with ScalaConvertible
{
	// IMPLEMENTED  ---------------------------
	
	def references = dataType.references.toSet
	
	override def toScala = {
		val defaultString = if (default.isEmpty) "" else s" = $default"
		s"$name: ${dataType.toScala}$defaultString"
	}
}
