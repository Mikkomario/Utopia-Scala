package utopia.echo.model.unit

import utopia.echo.model.enumeration.ByteCountUnit
import utopia.echo.model.enumeration.ByteCountUnit.{GigaBytes, MegaBytes}

/**
 * Common trait for factories that accept byte count units
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
trait FromByteCountUnitFactory[+A]
{
	// ABSTRACT ---------------------
	
	/**
	 * @param unit Applied unit
	 * @return A value in/using that unit
	 */
	def apply(unit: ByteCountUnit): A
	
	
	// COMPUTED ---------------------
	
	/**
	 * @return A value in/using GB
	 */
	def gigas = apply(GigaBytes)
	/**
	 * @return A value in/using MB
	 */
	def megas = apply(MegaBytes)
}
