package org.broadinstitute.sting.queue.extensions.queue

import org.broadinstitute.sting.queue.function.JavaCommandLineFunction
import java.io.File

/**
 * Wraps the qscripts
 */
trait QueueScriptFunction extends JavaCommandLineFunction {
    
  var script: File = null
  
  // TODO Make sure that this exists on the classpath, and can be accessed in this way.
  // Possibly one has to get the exact path to the file.
  this.jarFile = new File("dist/Queue.jar")
      
  abstract override def commandLine = super.commandLine +
  										required("-S", script)

}