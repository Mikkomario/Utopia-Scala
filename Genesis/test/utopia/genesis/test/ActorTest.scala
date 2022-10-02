package utopia.genesis.test

import java.time.Duration
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.test.TestContext._
import utopia.genesis.handling.immutable.ActorHandler
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable

import scala.concurrent.duration.FiniteDuration

object ActorTest extends App
{
    class TestActor extends mutable.Actor
    {
        var timeCounted = Duration.ZERO
        def millisCounted = timeCounted.toMillis
        override def act(duration: FiniteDuration) =
        {
            timeCounted += duration
            // println(timeCounted.toMillis)
        }
    }
    
    val actor1 = new TestActor()
    val actor2 = new TestActor()
    
    val handler = ActorHandler(actor1, actor2)
    val actorLoop = new ActorLoop(handler, 20 to 60)
    
    println(s"Interval: ${actorLoop.minInterval.toMillis} - ${actorLoop.maxInterval.toMillis}")
    
    assert(actor1.millisCounted == 0)
    assert(actor2.millisCounted == 0)
    
    
    
    actorLoop.runAsync()
    
    val waitDuration = 1.seconds
    Wait(waitDuration)
    
    actor2.isActive = false
    
    println((waitDuration - actor1.timeCounted).toMillis)
    val millis1 = actor1.millisCounted
    val millis2 = actor2.millisCounted
    
    println(millis1)
    println(millis2)
    
    assert(millis1 > 500)
    assert(millis1 < 1500)
    assert(millis2 > 500)
    assert(millis2 < 1500)
    
    Wait(1.seconds)
    
    assert(actor1.millisCounted > millis1 + 500)
    assert(actor2.millisCounted < millis2 + 500)
    
    println("Waiting for loop to end")
    actorLoop.stop().waitFor()
    
    println("Success")
}