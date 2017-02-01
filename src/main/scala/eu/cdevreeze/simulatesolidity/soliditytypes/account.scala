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
 * Account snapshot. Either an external account or contract account. Each "mutation" to the balance returns a
 * new Account object.
 *
 * @author Chris de Vreeze
 */
sealed abstract class Account(val address: Address, val balanceInWei: BigInt) {

  def addAmount(amount: BigInt): Account

  def subtractAmount(amount: BigInt): Account
}

final case class ExternalAccount(override val address: Address, override val balanceInWei: BigInt) extends Account(address, balanceInWei) {

  def addAmount(amount: BigInt): ExternalAccount = {
    new ExternalAccount(address, balanceInWei + amount)
  }

  def subtractAmount(amount: BigInt): ExternalAccount = {
    new ExternalAccount(address, balanceInWei - amount)
  }
}

final case class ContractAccount(override val address: Address, override val balanceInWei: BigInt) extends Account(address, balanceInWei) {

  def addAmount(amount: BigInt): ContractAccount = {
    new ContractAccount(address, balanceInWei + amount)
  }

  def subtractAmount(amount: BigInt): ContractAccount = {
    new ContractAccount(address, balanceInWei - amount)
  }
}
