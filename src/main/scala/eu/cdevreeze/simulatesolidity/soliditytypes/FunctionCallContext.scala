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
 * Function call context. See the documentation of trait Contract.
 *
 * @author Chris de Vreeze
 */
final class FunctionCallContext(
    val message: Message,
    val accountCollection: AccountCollection) extends HasAccountCollection {

  def messageSender: Address = message.messageSender

  def now: Instant = message.now

  def withMessage(newMessage: Message): FunctionCallContext = {
    new FunctionCallContext(newMessage, this.accountCollection)
  }
}

object FunctionCallContext {

  def apply(message: Message, accountCollection: AccountCollection): FunctionCallContext = {
    new FunctionCallContext(message, accountCollection)
  }
}
