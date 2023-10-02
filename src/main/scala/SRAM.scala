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
/*
 * SYNTHESIZES TO BLOCK RAM (or several block rams, depending on the depth)
 * (tested in vivado 2021.1)
 */
package memories
import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import chisel3.util.log2Up

class SRAMRead(depth: Int, width: Int) extends Bundle {
  val enable = Input(Bool())
  val address = Input(UInt(log2Up(depth).W))
  val data = UInt(width.W)
}
class SRAMWrite(depth: Int, width: Int) extends Bundle {
  val enable = Bool()
  val address = UInt(log2Up(depth).W)
  val data =  UInt(width.W)
}

class SRAM(depth: Int, width: Int = 32, hexFile: String = "") extends Module {  
  val io = IO(new Bundle {
    val read = new SRAMRead(depth, width)
    val write = Flipped(new SRAMWrite(depth, width))
  })
  
  val mem = SyncReadMem(depth, UInt(width.W))
  if (hexFile != "") {
    loadMemoryFromFileInline(mem, hexFile)
  }
  // Create one write port and one read port
  when(io.write.enable) {
    mem.write(io.write.address, io.write.data)
  }
  io.read.data := mem.read(io.read.address, io.read.enable)
}
