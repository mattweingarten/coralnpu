// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package freechips.rocketchip.util

import chisel3._
import chisel3.util._

// Minimal compatibility shim for CoralNPU sources that import
// freechips.rocketchip.util.AsyncQueue.
case class AsyncQueueParams(
    depth: Int = 8,
    sync: Int = 3,
    safe: Boolean = true,
    narrow: Boolean = false
)

object AsyncQueueParams {
  def singleton(sync: Int = 3, safe: Boolean = true): AsyncQueueParams =
    AsyncQueueParams(depth = 1, sync = sync, safe = safe, narrow = false)
}

class AsyncQueue[T <: Data](gen: T, params: AsyncQueueParams = AsyncQueueParams())
    extends RawModule {
  val io = IO(new Bundle {
    val enq_clock = Input(Clock())
    val enq_reset = Input(AsyncReset())
    val deq_clock = Input(Clock())
    val deq_reset = Input(AsyncReset())
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)
  })

  withClockAndReset(io.enq_clock, io.enq_reset) {
    val q = Module(new Queue(gen, entries = params.depth))
    q.io.enq <> io.enq
    io.deq <> q.io.deq
  }
}
