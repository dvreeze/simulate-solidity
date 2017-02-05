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
   * No rollback behavior has been implemented in this simple simulation.
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
