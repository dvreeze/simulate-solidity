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
    val funcCall: FunctionCallContext => HasAccountCollection) extends (AccountCollection => HasAccountCollection) {

  def apply(accountCollection: AccountCollection): HasAccountCollection = {
    funcCall(FunctionCallContext(message, accountCollection))
  }
}

object FunctionCall {

  def withoutWei(
    sender: Address,
    funcCall: FunctionCallContext => HasAccountCollection): FunctionCall = {

    new FunctionCall(Message.withoutWei(sender), funcCall)
  }
}
