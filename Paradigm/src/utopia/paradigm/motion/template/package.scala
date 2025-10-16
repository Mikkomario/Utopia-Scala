package utopia.paradigm.motion

import utopia.flow.operator.MayBeAboutZero

/**
 * @author Mikko Hilpinen
 * @since 16.10.2025, v1.8
 */
package object template
{
	@deprecated("Renamed to ChangeFactory", "v1.8")
	type ChangeFromModelFactory[+A, Amount <: MayBeAboutZero[Amount, _]] = ChangeFactory[A, Amount]
}
