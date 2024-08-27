package utopia.flow.test.event

import utopia.flow.test.TestContext._
import utopia.flow.view.mutable.eventful.{SettableFlag, ResettableFlag}

/**
  *
  * @author Mikko Hilpinen
  * @since 19/01/2024, v
  */
object FlagTest2 extends App
{
	private val _openedFlag = SettableFlag()
	private val _visibleFlag = ResettableFlag()
	private val _minimizedFlag = ResettableFlag()
	private val _closedFlag = SettableFlag()
	private val openFlag = _openedFlag && (!_closedFlag)
	
	private val fullyVisibleFlag = (_visibleFlag && (!_minimizedFlag)) && (!_closedFlag)
	
	openFlag.addContinuousListener { e => println(s"Open state $e (destiny = ${openFlag.destiny})") }
	_closedFlag.addContinuousListener { e => println(s"Closed state $e (destiny = ${_closedFlag.destiny})") }
	fullyVisibleFlag.addContinuousListener { e => println(s"Visible state $e (destiny = ${fullyVisibleFlag.destiny})") }
	
	println("Opening")
	_openedFlag.set()
	
	println("\nMaking visible")
	_visibleFlag.set()
	
	println("\nMinimizing")
	_minimizedFlag.set()
	
	println("\nRestoring")
	_minimizedFlag.reset()
	
	println("\nClosing")
	_closedFlag.set()
	
	println(s"\n${fullyVisibleFlag.destiny}")
}
