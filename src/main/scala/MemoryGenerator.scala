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
import java.nio.file.{Path, Paths}

/* Generates memories that are blocked to Block RAMs in FPGA.
 * It also handles placing the files in the correct location.
 * This is done via thread local variables, where each thread
 * can set the desired location of the files.
*/
object MemoryGenerator {
    val logger = LoggerFactory.getLogger("MemoryGenerator")
    
    private val memoryCounter = ThreadLocal.withInitial[Int](() => 0)
    private val genDir = new ThreadLocal[Path]
    def setGenDir(dir: Path) = {
        logger.debug(s"Setting generation directory to: ${dir.toString()}")
        genDir.set(dir)
    }
    def getGenDir() = genDir.get

    // Empty memory
    def SRAM(depth: Int, width: Int = 32): SRAM = {
        new SRAM(depth, width)
    }
    
    // Memory from a hexFile
    def SRAMInit(hexFile: String, width: Int = 32): SRAM = {
        if (!os.exists(os.Path(getGenDir().toString()))) {
            os.makeDir(os.Path(getGenDir().toString()))
        }
        val depth = os.read(os.Path(hexFile)).linesIterator.size
        val newName = s"mem${memoryCounter.get()}.hex"
        val newPath = Paths.get(getGenDir().toString(), newName).toAbsolutePath()
        os.copy.over(os.Path(hexFile), os.Path(newPath))
        logger.debug(s"Generating new memory from $hexFile -> ${getGenDir()}/$newName.")
        memoryCounter.set(memoryCounter.get + 1) 
        new SRAM(depth, width, s"${Paths.get(".").toAbsolutePath().relativize(getGenDir()).toString()}/$newName")
    }

    // Takes a hex string and saves it as a file
    def SRAMInitFromString(hexStr: String, width: Int = 32): SRAM = {
        if (!os.exists(os.Path(getGenDir().toString()))) {
            os.makeDir(os.Path(getGenDir().toString()))
        }
        val depth = hexStr.count(_ == '\n') + 1
        val fName = s"mem${memoryCounter.get()}.hex"
        val fPath = Paths.get(getGenDir().toString(), fName).toAbsolutePath()
        os.write.over(os.Path(fPath), hexStr)
        logger.debug(s"Generating new memory from string to file $fPath.")
        memoryCounter.set(memoryCounter.get + 1) 
        new SRAM(depth, width, s"${Paths.get(".").toAbsolutePath().relativize(getGenDir()).toString()}/$fName")
    }
}
