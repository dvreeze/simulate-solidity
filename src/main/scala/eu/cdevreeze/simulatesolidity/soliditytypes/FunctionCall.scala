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

import scala.util.Try

/**
 * Function call. Function calls can be collected into a "script".
 *
 * @author Chris de Vreeze
 */
final class FunctionCall(
    val message: Message,
    val recipient: Address,
    val funcCall: FunctionCallContext => HasAccountCollection) extends (AccountCollection => HasAccountCollection) {

  require(message.messageSender != recipient, s"Message sender and recipient must not be the same")

  /**
   * Invokes this function call. If WEI is sent with the message, this WEI is transferred from the balance
   * of the sender to the balance of the recipient contract account.
   *
   * For transactional behavior, see the other methods.
   */
  def apply(accountCollection: AccountCollection): HasAccountCollection = {
    require(
      accountCollection.accountsByAddress.get(message.messageSender).forall(_.balanceInWei >= message.messageValueInWei),
      s"Insufficient balance for message sender ${message.messageSender}")

    val updatedAccountCollection =
      if (message.messageValueInWei == BigInt(0)) {
        accountCollection
      } else {
        accountCollection.
          updated(message.messageSender, _.subtractAmount(message.messageValueInWei)).
          updated(recipient, _.addAmount(message.messageValueInWei))
      }

    funcCall(FunctionCallContext(message, updatedAccountCollection))
  }

  /**
   * "Transactional" call of this function. It is somewhat ACID minus durability. It is atomic in that an exception
   * rolls back the state to the previous function result and therefore account collection. It is isolated
   * in that only one thread at a time can call this function on this script object. As a result, the
   * state (mainly account collection) goes from consistent state to consistent state.
   *
   * To make isolation work, make sure that all potential simultaneous calls are protected by the same
   * lock object.
   */
  def call(accountCollection: AccountCollection)(lockObject: AnyRef): HasAccountCollection = lockObject.synchronized {
    Try(this(accountCollection)).toOption getOrElse {
      new FunctionResult((), accountCollection)
    }
  }

  /**
   * Like method callFunction, but returns the result wrapped in a Try, thus giving access to any thrown
   * exception.
   *
   * To make isolation work, make sure that all potential simultaneous calls are protected by the same
   * lock object.
   */
  def tryCalling(accountCollection: AccountCollection)(lockObject: AnyRef): Try[HasAccountCollection] = lockObject.synchronized {
    Try(this(accountCollection))
  }
}

object FunctionCall {

  def apply(message: Message, recipient: Address, funcCall: FunctionCallContext => HasAccountCollection): FunctionCall = {
    new FunctionCall(message, recipient, funcCall)
  }

  def withoutWei(
    sender: Address,
    recipient: Address,
    funcCall: FunctionCallContext => HasAccountCollection): FunctionCall = {

    new FunctionCall(Message.withoutWei(sender), recipient, funcCall)
  }
}
