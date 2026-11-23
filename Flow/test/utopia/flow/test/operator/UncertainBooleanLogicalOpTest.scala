package utopia.flow.test.operator

import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.{CertainlyFalse, CertainlyTrue}
import utopia.flow.view.immutable.View

/**
 * Tests logical operations with uncertain booleans
 * @author Mikko Hilpinen
 * @since 22.11.2026, v2.9
 */
object UncertainBooleanLogicalOpTest extends App
{
	private val t = CertainlyTrue
	private val f = CertainlyFalse
	private val u = UncertainBoolean
	
	private val tv = View.fixed(t)
	private val fv = View.fixed(f)
	private val uv = View.fixed(u)
	private val notNeeded = View { throw new IllegalStateException("This parameter shouldn't be called") }
	
	// !
	assert(!t == f)
	assert(!f == t)
	assert(!u == u)
	
	// &&
	
	assert((t && tv) == t)
	assert((t && fv) == f)
	assert((t && uv) == u)
	assert((f && notNeeded) == f)
	assert((u && tv) == u)
	assert((u && fv) == f)
	assert((u && uv) == u)
	
	// ||
	
	assert((t || notNeeded) == t)
	assert((f || tv) == t)
	assert((f || fv) == f)
	assert((f || uv) == u)
	assert((u || tv) == t)
	assert((u || fv) == u)
	assert((u || uv) == u)
	
	println("Success!")
}
