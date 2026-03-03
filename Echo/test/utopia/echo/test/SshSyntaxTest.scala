package utopia.echo.test

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.Env

/**
 *
 * @author Mikko Hilpinen
 * @since 03.03.2026, v
 */
object SshSyntaxTest extends App
{
	private val baseArgs = Vector(
		"-i", (Env.home.get/".ssh/id_ed25519").toString,
		"-p", "<port>",
		"-o", "BatchMode=yes",
		"-o", "StrictHostKeyChecking=no",
		"-o", "UserKnownHostsFile=/dev/null",
	)
	private val sshBase = Vector.concat(Single("ssh"), baseArgs, Single("root@<host>"))
	private lazy val scpBase = "scp" +: baseArgs
	
	println(sshBase)
	println(scpBase)
	println(s"chmod +x <path> && <path>${ Pair("arg1", "arg2").iterator.map { arg => s" '$arg'" }.mkString }")
	println("-N -L <localport>:<host>:<remoteport> -o ServerAliveInterval=60 -o ServerAliveCountMax=5")
}
