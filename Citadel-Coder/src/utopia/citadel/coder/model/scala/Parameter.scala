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
	
	def references = dataType.references
	
	override def toScala = {
		val prefixString = if (prefix.isEmpty) "" else s"$prefix "
		val defaultString = if (default.isEmpty) "" else s" = $default"
		s"$prefixString$name: ${dataType.toScala}$defaultString"
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new parameter list by adding implicits to this parameter
	  * @param firstParam First implicit parameter
	  * @param moreParams More implicit parameters
	  * @return A new parameters list
	  */
	def withImplicits(firstParam: Parameter, moreParams: Parameter*) =
		Parameters(Vector(Vector(this)), firstParam +: moreParams.toVector)
}
