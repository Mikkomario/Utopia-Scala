package utopia.nexus.test

import utopia.nexus.http.Path

/**
 * This app tests use of Paths
 * @author Mikko Hilpinen
 * @since 22.8.2017
 */
object PathTest extends App
{
    val path = Path("a", "b", "c")
    
    assert(path == Path(Vector("a", "b", "c")))
    assert(path.head == "a")
    assert(path.tail.contains(Path("b", "c")))
    assert(path.tail.get.tail.get.tail.isEmpty)
    assert(path/"d" == Path("a", "b", "c", "d"))
    assert(path.prepend("x") == Path("x", "a", "b", "c"))
    assert(Path.parse(path.toString).get == path)
    assert(path.lastElement == "c")
    
    assert(path.before(Path("b", "c")).get == Path("a"))
    assert(path.after(Path("a", "b")).get == Path("c"))
    assert(path.before(Path("a", "b")).isEmpty)
    assert(path.after(Path("b", "c")).isEmpty)
    assert(path.before(Path("x", "y")).isEmpty)
    assert(path.before(Path("x", "y")).isEmpty)
    
    assert(Some(path)/"d" == Path("a", "b", "c", "d"))
    assert(Path("a").tail/"d" == Path("d"))
    
    println("Success!")
}