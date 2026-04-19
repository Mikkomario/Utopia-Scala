package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.result.MayHaveFailed
import utopia.flow.util.result.TryExtensions._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Tests Future + Try features
 * @author Mikko Hilpinen
 * @since 19.04.2026, v2.9
 */
object TryFutureTest extends App
{
	// TESTS    ----------------------
	
	// Tests currentResult()
	{
		val fs = success
		val fs2 = rawSuccess
		val ff = failure
		val ff2 = throwing
		
		assert(fs.currentResult.isEmpty)
		assert(fs2.currentResult.isEmpty)
		assert(ff.currentResult.isEmpty)
		assert(ff2.currentResult.isEmpty)
		
		Wait(0.2.seconds)
		
		assert(fs.currentResult.get.isSuccess)
		assert(fs2.currentResult.get.isSuccess)
		assert(ff.currentResult.get.isFailure)
		assert(ff2.currentResult.get.isFailure)
	}
	
	// Tests hasSucceeded & hasFailed
	{
		val fs = success
		val fs2 = rawSuccess
		val ff = failure
		val ff2 = throwing
		
		assert(!fs.hasSucceeded)
		assert(!fs.hasFailed)
		assert(!fs2.hasSucceeded)
		assert(!fs2.hasFailed)
		assert(!ff.hasSucceeded)
		assert(!ff.hasFailed)
		assert(!ff2.hasSucceeded)
		assert(!ff2.hasFailed)
		
		Wait(0.2.seconds)
		
		assert(fs.hasSucceeded)
		assert(!fs.hasFailed)
		assert(fs2.hasSucceeded)
		assert(!fs2.hasFailed)
		assert(!ff.hasSucceeded)
		assert(ff.hasFailed)
		assert(!ff2.hasSucceeded)
		assert(ff2.hasFailed)
	}
	
	// Tests unwrapped
	{
		val s = success.unwrapped
		val f = failure.unwrapped
		
		assert(s.waitFor().isSuccess)
		assert(f.waitFor().isFailure)
	}
	
	// Tests toTryFuture
	{
		val s = rawSuccess.toTryFuture
		val f = throwing.toTryFuture
		
		assert(s.waitFor().get.isSuccess)
		assert(f.waitFor().get.isFailure)
	}
	
	// Tests mapSuccess
	{
		val s = success.mapSuccess { _ + 1 }
		val f = failure.mapSuccess { _ + 1 }
		
		assert(s.waitForResult().get == 2)
		assert(f.waitForResult().isFailure)
	}
	
	// Tests flatMapSuccess
	{
		val ss = success.flatMapSuccess { a => rawSuccess.map { _ + a } }
		val sf = success.flatMapSuccess { a => throwing.map { _ + a } }
		val fs = failure.flatMapSuccess { a => rawSuccess.map { _ + a } }
		
		Wait(0.15.seconds)
		
		assert(!ss.isCompleted)
		assert(!sf.isCompleted)
		assert(fs.isCompleted)
		
		assert(ss.waitForResult().get == 2)
		assert(sf.waitForResult().isFailure)
		assert(fs.waitForResult().isFailure)
	}
	
	// Tests mapOrFail
	testTryMapLike { (from, f) => from.mapOrFail { f(_) } }
	
	// Tests flatMapOrFail
	testTryFlatMapLike { (from, f) => from.flatMapOrFail { f(_).map { r => r } } }
	
	// Tests tryMap and tryFlatMap
	testTryMapLike { _.tryMap(_) }
	testTryFlatMapLike { _.tryFlatMap(_) }
	
	// Tests mapFailure
	{
		val ff = failure.mapFailure { _ => immediateFailure }
		val fs = failure.mapFailure { _ => Success(1) }
		val ss = success.mapFailure { _ => Success(2) }
		
		assert(ff.waitForResult().isFailure)
		assert(fs.waitForResult().get == 1)
		assert(ss.waitForResult().get == 1)
	}
	
	// Tests flatMapFailure
	{
		val ff = failure.flatMapFailure { _ => failure.map { r => r: MayHaveFailed[Int] } }
		val fs = failure.flatMapFailure { _ => success.map { r => r: MayHaveFailed[Int] } }
		val ss = success.flatMapFailure { _ => success.map { _.map { _ + 1 }: MayHaveFailed[Int] } }
		
		Wait(0.15.seconds)
		
		assert(!ff.isCompleted)
		assert(!fs.isCompleted)
		assert(ss.isCompleted)
		
		assert(ff.waitForResult().isFailure)
		assert(fs.waitForResult().get == 1)
		assert(ss.waitForResult().get == 1)
	}
	
	// Tests tryMapFailure
	{
		val ff = failure.tryMapFailure { _ => immediateFailure }
		val fs = failure.tryMapFailure { _ => Success(1) }
		val ss = success.tryMapFailure { _ => Success(2) }
		
		assert(ff.waitForResult().isFailure)
		assert(fs.waitForResult().get == 1)
		assert(ss.waitForResult().get == 1)
	}
	
	// Tests tryFlatMapFailure
	{
		val ff = failure.tryFlatMapFailure { _ => failure }
		val fs = failure.tryFlatMapFailure { _ => success }
		val ss = success.tryFlatMapFailure { _ => success.mapSuccess { _ + 1 } }
		
		Wait(0.15.seconds)
		
		assert(!ff.isCompleted)
		assert(!fs.isCompleted)
		assert(ss.isCompleted)
		
		assert(ff.waitForResult().isFailure)
		assert(fs.waitForResult().get == 1)
		assert(ss.waitForResult().get == 1)
	}
	
	// Tests forSuccess, forFailure && forResult
	{
		testForResult(success, expectSuccess = true)
		testForResult(failure, expectSuccess = false)
		testForResult(delayed { throw new IllegalStateException("Failure") }, expectSuccess = false)
	}
	
	// Tests withTimeout
	{
		val ss = success.withTimeout(0.2.seconds)
		val sf = success.withTimeout(0.02.seconds)
		val fs = failure.withTimeout(0.2.seconds)
		val ff = failure.withTimeout(0.02.seconds)
		
		Wait(0.05.seconds)
		
		assert(!ss.isCompleted)
		assert(sf.isCompleted)
		assert(!fs.isCompleted)
		assert(ff.isCompleted)
		
		assert(ss.waitForResult().isSuccess)
		assert(sf.waitForResult().isFailure)
		assert(fs.waitForResult().isFailure)
		assert(ff.waitForResult().isFailure)
	}
	
	
	// OTHER    ----------------------
	
	private def testTryMapLike(f: (Future[Try[Int]], Int => Try[Int]) => Future[Try[Int]]) = {
		val ss = f(success, { a => Success(a + 1) })
		val sf = f(success, { _ => immediateFailure })
		val fs = f(failure, { a => Success(a + 1) })
		
		assert(ss.waitForResult().get == 2)
		assert(sf.waitForResult().isFailure)
		assert(fs.waitForResult().isFailure)
	}
	private def testTryFlatMapLike(f: (Future[Try[Int]], Int => Future[Try[Int]]) => Future[Try[Int]]) = {
		val ss = f(success, { a => delayed { Success(a + 1) } })
		val sf = f(success, { _ => delayed(immediateFailure) })
		val fs = f(failure, { a => delayed { Success(a + 1) } })
		
		Wait(0.15.seconds)
		
		assert(!ss.isCompleted)
		assert(!sf.isCompleted)
		assert(fs.isCompleted)
		
		assert(ss.waitForResult().get == 2)
		assert(sf.waitForResult().isFailure)
		assert(fs.waitForResult().isFailure)
	}
	
	private def testForResult(future: Future[Try[Int]], expectSuccess: Boolean) = {
		var successes = 0
		var failures = 0
		
		future.forSuccess { _ => successes += 1 }
		future.forFailure { _ => failures += 1 }
		future.forResult {
			case Success(_) => successes += 1
			case Failure(_) => failures += 1
		}
		
		Wait(0.2.seconds)
		
		if (expectSuccess) {
			assert(successes == 2)
			assert(failures == 0)
		}
		else {
			assert(successes == 0)
			assert(failures == 2)
		}
	}
	
	private def rawSuccess = delayed(1)
	private def success: Future[Try[Int]] = delayed { Success(1) }
	
	private def throwing: Future[Int] = delayed { throw new IllegalStateException("TEST (thrown)") }
	private def failure: Future[Try[Int]] = delayed(immediateFailure)
	private def immediateFailure = Failure(new IllegalStateException("TEST failure"))
	
	private def delayed[A](result: => A) = Delay(0.1.seconds)(result)
}
