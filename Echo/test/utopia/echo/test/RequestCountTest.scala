package utopia.echo.test

import utopia.echo.model.tokenization.RequestCount

/**
 * @author Mikko Hilpinen
 * @since 23.11.2026, v1.6
 */
object RequestCountTest extends App
{
	assert(RequestCount(1, 2) + RequestCount(3, 4) == RequestCount(4, 6))
	assert(RequestCount(3, 4) - RequestCount(1, 2) == RequestCount(2, 2))
	
	println("Success!")
}
