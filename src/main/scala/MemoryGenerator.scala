/*
 * Copyright 2022 Computer Systems Department, Jozef Stefan Insitute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package memories
import org.slf4j.LoggerFactory
import os._
import chisel3._
import chisel3.experimental.BundleLiterals._

/* Generates memories that are blocked to Block RAMs in FPGA.
 * It also handles placing the files in the correct location.
 * This is done via thread local variables, where each thread
 * can set the desired location of the files.
*/
object MemoryGenerator {
    val logger = LoggerFactory.getLogger("MemoryGenerator")
    
    private val memoryCounter = ThreadLocal.withInitial[Int](() => 0)
    private val genDir = new ThreadLocal[os.Path]
    def setGenDir(dir: os.Path) = {
        logger.debug(s"Setting generation directory to: ${dir.toString()}")
        genDir.set(dir)
    }
    def getGenDir = genDir.get

    // Empty memory
    def SRAM(depth: Int, width: Int = 32, noWritePort: Boolean = false): SRAM = {
      new SRAM(depth, width, noWritePort = noWritePort)
    }
   
    def getTieOffBundle(depth: Int, width: Int) = (new SRAMWrite(depth, width)).Lit(_.enable -> false.B, 
                                                                                    _.address -> 0.U, 
                                                                                    _.data -> 0.U)
    // Memory from a hexFile
    def SRAMInit(hexFile: String, width: Int = 32, noWritePort: Boolean = false): SRAM = {
      if (!os.exists(getGenDir)) {
          os.makeDir(getGenDir)
      }
      val depth = os.read(os.Path(hexFile)).linesIterator.size
      val newName = s"mem${memoryCounter.get()}.hex"
      val newPath = getGenDir / newName
      os.copy.over(os.Path(hexFile), newPath)
      logger.debug(s"Generating new memory from $hexFile -> ${getGenDir}/$newName.")
      memoryCounter.set(memoryCounter.get + 1) 
      new SRAM(depth, width, s"${getGenDir.relativeTo(os.pwd).toString()}/$newName", noWritePort=noWritePort)
    }

    // Takes a hex string and saves it as a file
    def SRAMInitFromString(hexStr: String, isBinary: Boolean = false, width: Int = 32, noWritePort: Boolean = false): SRAM = {
      if (!os.exists(getGenDir)) {
          os.makeDir(getGenDir)
      }
      val depth = hexStr.count(_ == '\n') + 1
      val fName = s"mem${memoryCounter.get()}.${if (isBinary) "bin" else "hex"}"
      val fPath = getGenDir / fName
      os.write.over(fPath, hexStr)
      logger.debug(s"Generating new memory from string to file $fPath.")
      memoryCounter.set(memoryCounter.get + 1) 
      new SRAM(depth, width, s"${getGenDir.relativeTo(os.pwd).toString()}/$fName", isBinary, noWritePort)
    }
}
