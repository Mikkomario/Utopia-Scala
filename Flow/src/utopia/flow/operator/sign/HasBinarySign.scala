package utopia.flow.operator.sign

/**
  * Common trait for items that can be positive or negative, but never zero
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
trait HasBinarySign extends HasSign
{
	// ABSTRACT ------------------------
	
	override def sign: Sign
}
