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
 * Address. Unlike an address in Solidity, it is just the immutable address, without any mutable
 * state such as a balance. Hence there is no send method on class Address itself.
 *
 * @author Chris de Vreeze
 */
final case class Address(val addressValue: Int)

object Address {

  def send(to: Address, amount: Int): Boolean = {
    // Just a stub
    true
  }
}
