package utopia.flow.test.file

import utopia.flow.generic.DataType
import utopia.flow.util.FileExtensions._

import java.nio.file.Path

/**
 * Tests file operations
 * @author Mikko Hilpinen
 * @since 8.1.2020, v1.6.1
 */
object FileTest extends App
{
	DataType.setup()
	
	val origin: Path = "test-data"
	val dir1 = origin / "test-dir-1"
	val dir2 = origin / "test-dir-2"
	val jsonOriginal = dir1 / "test.json"
	
	// Tests path conversion
	assert(jsonOriginal.withFileName("test2.json") == dir1 / "test2.json")
	
	// Tests exists
	assert(dir1.exists)
	assert(dir2.exists)
	assert(jsonOriginal.exists)
	assert((dir1 / "notExists.json").notExists)
	assert(dir1.isDirectory)
	assert(dir2.isDirectory)
	assert(jsonOriginal.isRegularFile)
	
	// Tests File name, type & last modified
	assert(jsonOriginal.fileName == "test.json")
	assert(jsonOriginal.lastModified.isSuccess)
	assert(jsonOriginal.fileType == "json")
	assert(jsonOriginal.size.isSuccess)
	
	// Tests directory creation
	val newDir = origin / "test-dir-new"
	assert(newDir.notExists)
	assert(newDir.asExistingDirectory.get == newDir)
	assert(newDir.exists)
	assert(newDir.isDirectory)
	assert(jsonOriginal.asExistingDirectory.isFailure)
	
	// Tests parents & children
	assert(jsonOriginal.parent == dir1)
	assert(dir1.children.get.contains(jsonOriginal))
	assert(origin.subDirectories.get.contains(dir1))
	assert(!dir1.subDirectories.get.contains(jsonOriginal))
	
	// Tests copy, move etc.
	// Copies test.json as json2.json
	val jsonCopy = jsonOriginal.copyAs(dir1 / "json2.json").get
	assert(jsonCopy.exists)
	assert(jsonCopy.isRegularFile)
	assert(jsonCopy.fileType == "json")
	
	// Copies dir1
	val dir1Copy = dir1.copyAs(origin / "test-dir-1-copy").get
	assert(dir1Copy.exists)
	assert(dir1Copy.isDirectory)
	assert((dir1Copy / "json2.json").exists)
	
	// Copies test.json to dir2
	val jsonCopy2 = jsonOriginal.copyTo(dir2).get
	assert(jsonCopy2.exists)
	
	// Renames json2.json to json-copy.json in dir2
	val renamedJsonCopy2 = jsonCopy2.rename("json-copy.json").get
	assert(renamedJsonCopy2.exists)
	assert(jsonCopy2.notExists)
	assert(jsonOriginal.rename("json2.json").isFailure)
	
	// Moves json-copy.json from copied dir1 to original dir1
	val movedJsonCopy2 = renamedJsonCopy2.moveTo(dir1).get
	assert(movedJsonCopy2.exists)
	assert(renamedJsonCopy2.notExists)
	
	// Overwrites test.json in dir2 with json2.json in dir1
	val overWrittenDir2Json = jsonCopy2.overwriteWith(jsonCopy).get
	assert(jsonCopy2.notExists)
	assert(overWrittenDir2Json.exists)
	
	// Moves copied dir1 to original dir1
	val movedDir1Copy = dir1Copy.moveTo(dir1).get
	assert(dir1Copy.notExists)
	assert(movedDir1Copy.exists)
	assert((movedDir1Copy / "test.json").exists)
	
	// Overwrites this new directory with dir2
	val overwrittenDir1Copy = movedDir1Copy.overwriteWith(dir2).get
	assert(overwrittenDir1Copy.exists)
	assert(dir2.exists)
	assert((overwrittenDir1Copy / "test.json").notExists)
	assert((overwrittenDir1Copy / "json2.json").exists)
	assert(movedDir1Copy.notExists)
	
	// Copies original json file to non-existing directory
	val jsonCopy3 = jsonOriginal.copyTo(origin / "new-directory").get
	assert(jsonCopy3.exists)
	
	// Deletes unnecessary files
	assert(newDir.delete().get)
	assert((origin / "new-directory").delete().get)
	assert(overwrittenDir1Copy.delete().get)
	assert(jsonCopy.delete().get)
	assert(movedJsonCopy2.delete().get)
	assert(overWrittenDir2Json.delete().get)
	
	println("Success")
}
