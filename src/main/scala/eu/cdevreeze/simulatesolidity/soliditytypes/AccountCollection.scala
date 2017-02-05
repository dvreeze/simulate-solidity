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
 * Snapshot of the collection of all accounts. It is part of the required context passed to each smart contract
 * function call, but also returned from the function call.
 *
 * @author Chris de Vreeze
 */
final class AccountCollection(val accountsByAddress: Map[Address, Account]) {
  require(accountsByAddress forall { case (addr, acc) => acc.address == addr }, s"Corrupt account collection")

  def updated(address: Address, f: Account => Account): AccountCollection = {
    if (accountsByAddress.contains(address)) {
      new AccountCollection(accountsByAddress.updated(address, f(accountsByAddress(address))))
    } else {
      this
    }
  }

  def send(from: Address, to: Address, amount: BigInt): Option[AccountCollection] = {
    require(amount >= 0, s"Negative amount $amount not allowed")

    val fromAccountOption = accountsByAddress.get(from)
    val toAccountOption = accountsByAddress.get(to)

    // Assume non-failure here, because this simulation has no notion of gas and no call stack limit (other than the one of the JVM)

    if (fromAccountOption.isEmpty || toAccountOption.isEmpty) {
      None
    } else {
      if (fromAccountOption.get.balanceInWei < amount) {
        None
      } else {
        val newAccountsByAddress =
          accountsByAddress ++ Map(
            from -> fromAccountOption.get.subtractAmount(amount),
            to -> toAccountOption.get.addAmount(amount))

        // The "update" in one place. Still this does not enforce AC(I)D transactionality.
        Some(new AccountCollection(newAccountsByAddress))
      }
    }
  }
}
