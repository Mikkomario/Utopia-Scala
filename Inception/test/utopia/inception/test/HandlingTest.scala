package utopia.inception.test

import utopia.inception.handling.mutable

import utopia.inception.handling.Handler
import utopia.inception.handling.mutable.HandlerRelay

object HandlingTest extends App
{
    def countHandedObjects(handler: Handler[_]) =
    {
        var amount = 0
        handler.handle { _ => amount += 1 }
        amount
    }
    
    val handlerType = TestHandlerType
    
    val o1 = new TestObject(1)
    val o2 = new TestObject(2)
    val o3 = new TestObject(3)
    
    assert(!o1.isDead)
    assert(o1.defaultHandlingState)
    assert(o1.handlingState(handlerType))
    
    o1.specifyHandlingState(handlerType, false)
    assert(!o1.handlingState(handlerType))
    assert(o1.defaultHandlingState)
    
    val handler1: mutable.Handler[TestObject] = mutable.Handler[TestObject](TestHandlerType)
    val relay = HandlerRelay(handler1)
    
    assert(relay.handlers.nonEmpty)
    assert(handler1.isEmpty)
    
    handler1 ++= (o2, o3, o1)
    
    handler1.sortWith({ _.index <= _.index })
    assert(handler1.head == o1)
    handler1.sortWith({ _.index >= _.index })
    assert(handler1.head == o3)
    
    assert(handler1.size == 3)
    assert(countHandedObjects(handler1) == 2)
    
    o1.specifyHandlingState(handlerType, true)
    assert(o1.handlingState(handlerType))
    assert(countHandedObjects(handler1) == 3)
    
    o1.defaultHandlingState = false
    assert(o1.handlingState(handlerType))
    assert(countHandedObjects(handler1) == 3)
    
    o2.defaultHandlingState = false
    assert(!o2.handlingState(handlerType))
    assert(countHandedObjects(handler1) == 2)
    
    o1.kill()
    assert(handler1.considersDead(o1))
    assert(countHandedObjects(handler1) == 1)
    assert(handler1.size == 2)
    
    println("Success!")
}