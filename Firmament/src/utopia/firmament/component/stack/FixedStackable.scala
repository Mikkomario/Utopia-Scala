package utopia.firmament.component.stack

/**
 * Common trait for stackable components that don't change their stack size
 * @author Mikko Hilpinen
 * @since 07.08.2025, v1.6
 */
trait FixedStackable extends Stackable
{
	override def resetCachedSize(): Unit = ()
	override def updateStackSize(): Boolean = false
}
