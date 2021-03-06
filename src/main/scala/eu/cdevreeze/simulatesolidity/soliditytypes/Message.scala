/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.simulatesolidity.soliditytypes

import java.time.Instant

/**
 * Message. If the message contains non-zero WEI, this must be added to the account receiving the message.
 */
final class Message(
    val messageSender: Address,
    val messageValueInWei: BigInt) {

  require(messageValueInWei >= 0, s"The message value must be non-negative, but got $messageValueInWei instead")

  val now: Instant = Instant.now() // Incorrect, because this must be a block property
}

object Message {

  def apply(messageSender: Address, messageValueInWei: BigInt): Message = {
    new Message(messageSender, messageValueInWei)
  }

  def withoutWei(messageSender: Address): Message = {
    new Message(messageSender, 0)
  }
}
