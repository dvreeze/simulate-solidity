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

package eu.cdevreeze.simulatesolidity.contracts

import java.util.logging.Logger

import eu.cdevreeze.simulatesolidity.aspects.SenderAspects
import eu.cdevreeze.simulatesolidity.collections.Updater
import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.Contract
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionCallContext
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionResult

/**
 * Simulation of a cryptocurrency Solidity example.
 *
 * See https://solidity.readthedocs.io/en/develop/introduction-to-smart-contracts.html.
 *
 * The class is thread-safe.
 *
 * @author Chris de Vreeze
 */
final class Coin(
    initialBalances: Map[Address, Coin.Balance])(val firstContext: FunctionCallContext, val ownAddress: Address) extends Contract with SenderAspects {
  require(initialBalances.nonEmpty, s"There must be at least one balance")

  private val logger: Logger = Logger.getGlobal

  val minter: Address = firstContext.messageSender

  // Balance "table". It is mutable, but only the reference, whereas the snapshot itself is immutable.
  var balances: Map[Address, Coin.Balance] = {
    initialBalances
  }

  // No class invariant

  /**
   * Mints the given amount for the given receiver. Only callable by the minter.
   */
  def mint(receiver: Address, amount: Int)(context: FunctionCallContext): FunctionResult[Unit] = this.synchronized {
    withRequiredSender(minter)(context) { () =>
      updateBalance(receiver, (_.add(amount)))

      FunctionResult.fromCallContextOnly(context)
    }
  }

  /**
   * Sends the given amount of money (in "coins") to the given receiver.
   */
  def send(receiver: Address, amount: Int)(context: FunctionCallContext): FunctionResult[Unit] = this.synchronized {
    require(amount >= 0, s"One cannot send a negative amount of money")
    require(receiver != context.messageSender, s"The receiver must differ from the sender ${context.messageSender}")

    require(balances(context.messageSender).value >= amount, s"Insufficient funds")

    updateBalance(context.messageSender, (_.subtract(amount)))
    updateBalance(receiver, (_.add(amount)))

    logger.info(s"Sent $amount from ${context.messageSender} to ${receiver}")

    FunctionResult.fromCallContextOnly(context)
  }

  private def updateBalance(address: Address, f: Coin.Balance => Coin.Balance): Unit = {
    balances = Updater.updated(balances, address, f)
  }
}

object Coin {

  final case class Balance(val value: Int) {

    def add(amount: Int): Balance = {
      Coin.Balance(this.value + amount)
    }

    def subtract(amount: Int): Balance = {
      add(-amount)
    }
  }
}
