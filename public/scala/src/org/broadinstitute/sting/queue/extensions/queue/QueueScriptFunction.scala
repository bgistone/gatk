package org.broadinstitute.sting.queue.extensions.queue

import org.broadinstitute.sting.queue.function.JavaCommandLineFunction
import java.io.File
import org.broadinstitute.sting.commandline._

/**
 * Wraps the qscripts
 */
trait QueueScriptFunction extends JavaCommandLineFunction {
  
  	@Argument(doc="Run the qscript", fullName="run", shortName="r", required=false)
  	var run: Boolean = false   
    
  	@Argument(doc="Debug", fullName="debug", shortName="debug", required=false)
  	var debug: Boolean = false   
  	
  	@Output(doc="Standard Output", shortName = "o", fullName = "std_out", required = false)
	var stdOut: File = _
  	
  	var script: File = null
  
  	// TODO Make sure that this exists on the classpath, and can be accessed in this way.
  	// Possibly one has to get the exact path to the file.
  	this.jarFile = new File("dist/Queue.jar")
      
  	abstract override def commandLine = super.commandLine +
  										required("-S", script) +
  										conditional(run, "-run") +
  										conditional(debug, "-l DEBUG")

}