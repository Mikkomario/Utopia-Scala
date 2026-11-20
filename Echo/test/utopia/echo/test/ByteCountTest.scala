package utopia.echo.test

import utopia.echo.model.unit.ByteCountExtensions._

/**
 * Tests certain ByteCount functions (work in progress)
 * @author Mikko Hilpinen
 * @since 02.03.2026, v1.5
 */
object ByteCountTest extends App
{
	assert((10.gib + 5.gib).gigas == 15)
}
