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
 * Contract super-type of all "smart contracts".
 *
 * Each public method must have a 2nd parameter list with one parameter of type FunctionCallContext,
 * and a function result wrapped in a FunctionResult object. Each constructor also takes the extra
 * FunctionCallContext parameter (list). To return a FunctionResult during construction, use a
 * ContractBuilder, whose build method calls the contract constructor and obeys the same requirements
 * mentioned above as public contract methods.
 *
 * @author Chris de Vreeze
 */
trait Contract {

  def ownAddress: Address
}
