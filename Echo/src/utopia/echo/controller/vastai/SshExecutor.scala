package utopia.echo.controller.vastai

import utopia.echo.model.vastai.instance.SshConnection
import utopia.flow.collection.immutable.{Empty, Pair, Single}

import java.nio.file.Path
import scala.sys.process.Process

/**
 * An interface which provides functions for executing commands over SSH
 * @param config Configuration for connecting to the remote device via SSH
 * @param privateKeyPath Path to the private SSH key file to use
 * @author Mikko Hilpinen
 * @since 02.03.2026, v1.5
 */
class SshExecutor(config: SshConnection, privateKeyPath: Path)
{
	// ATTRIBUTES   ------------------------
	
	private val baseArgs = Vector(
		"-i", privateKeyPath.toString,
		"-p", config.port.toString,
		"-o", "BatchMode=yes",
		"-o", "StrictHostKeyChecking=no",
		"-o", "UserKnownHostsFile=/dev/null",
	)
	private val sshBase = Vector.concat(Single("ssh"), baseArgs, Single(s"root@${config.host}"))
	private lazy val scpBase = "scp" +: baseArgs
	
	
	// OTHER    ---------------------------
	
	/**
	 * Prepares a command to be executed over SSH
	 * @param command Command to execute
	 * @return Process representing that command
	 */
	def apply(command: String) = Process(sshBase :+ command)
	
	/**
	 * Prepares a command for transferring a file to a remote device using SCP
	 * @param file File to transfer
	 * @param toRemotePath Path to the file on the remote device
	 * @return Process for executing the transfer
	 */
	def transfer(file: Path, toRemotePath: String) =
		Process(scpBase ++ Pair(file.toString, s"root@${ config.host }:$toRemotePath"))
	/**
	 * Prepares a command for executing a file over SSH
	 * @param remoteScriptPath Path to the script / file on the remote instance, which should be executed
	 * @param args Arguments to pass to the execution
	 * @return A process for executing that script / file
	 */
	def executeScript(remoteScriptPath: String, args: Seq[String] = Empty) =
		apply(s"chmod +x $remoteScriptPath && $remoteScriptPath${ args.iterator.map { arg => s" '$arg'" }.mkString }")
	
	/**
	 * Prepares a command for initiating port-forwarding
	 * @param localPort Local port that will be forwarded to a remote port
	 * @param remotePort Remote port the local port will match
	 * @param remoteHost Targeted host on the remote device. Default = 127.0.0.1 (i.e. localhost).
	 * @return A process for starting / running the port-forwarding (will run as long as forwarding is active)
	 */
	def portForwarding(localPort: Int, remotePort: Int, remoteHost: String = "127.0.0.1") =
		apply(s"-N -L $localPort:$remoteHost:$remotePort -o ServerAliveInterval=60 -o ServerAliveCountMax=5")
}
