package utopia.vault.nosql.targeting.one

/**
  * Common trait for wrappers that provide an advanced interface to an access point that targets individual instances
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait AccessOneWrapper[A, +Repr <: TargetingOne[A]]
	extends TargetingOne[A] with TargetingOneWrapper[TargetingOne[A], A, A, Repr]
{
	override protected def wrapResult(result: A): A = result
}
