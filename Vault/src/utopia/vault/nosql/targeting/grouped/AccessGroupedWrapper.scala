package utopia.vault.nosql.targeting.grouped

/**
 * Common trait for extended (grouped) access points which implement [[AccessGrouped]] by wrapping another instance,
 * usually adding more functions and/or properties to it.
 * @author Mikko Hilpinen
 * @since 21.01.2026, v2.1
 */
trait AccessGroupedWrapper[A, +Repr <: TargetingGrouped[A]]
	extends TargetingGrouped[A] with TargetingGroupedWrapper[TargetingGrouped[A], A, A, Repr]
{
	override protected def wrapResult(result: A): A = result
}
