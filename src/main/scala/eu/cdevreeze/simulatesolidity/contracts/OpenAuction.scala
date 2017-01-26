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

import java.time.Instant
import java.util.logging.Logger

import scala.collection.immutable

import eu.cdevreeze.simulatesolidity.aspects.SenderAspects
import eu.cdevreeze.simulatesolidity.aspects.TimeAspects
import eu.cdevreeze.simulatesolidity.collections.Updater
import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.Context

/**
 * Simulation of the open auction Solidity example.
 *
 * See https://solidity.readthedocs.io/en/develop/solidity-by-example.html.
 *
 * The class is thread-safe.
 *
 * @author Chris de Vreeze
 */
final class OpenAuction(
    val beneficiary: Address,
    val biddingDurationInSeconds: Int)(val firstContext: Context) extends SenderAspects with TimeAspects {

  private val logger: Logger = Logger.getGlobal

  val auctionStartTime: Instant = Instant.now()

  private val auctionEndTime: Instant = {
    auctionStartTime.plusSeconds(biddingDurationInSeconds)
  }

  var highestBidderOption: Option[Address] = None

  var highestBid: Int = 0

  var pendingReturns: Map[Address, Int] = Map()

  var ended: Boolean = false

  /**
   * Bid on the auction with the value sent with the transaction. This function is "payable".
   */
  def bid()(context: Context): Unit = this.synchronized {
    notAfter(auctionEndTime)(context) { () =>
      require(context.messageValue > highestBid, s"Bid below highest bid. Does a refund.")

      if (highestBidderOption.isDefined) {
        // Pull instead of push money transfer...
        updatePendingReturns(highestBidderOption.get, (_ + highestBid))
      }

      highestBidderOption = Some(context.messageSender)
      highestBid = context.messageValue

      logger.info(s"Highest bid increased. Sender: ${context.messageSender}. Value: ${highestBid}")
    }
  } ensuring (_ => requireInvariant(context))

  def withdraw()(context: Context): Boolean = this.synchronized {
    val amount = pendingReturns.getOrElse(context.messageSender, 0)

    if (amount > 0) {
      // This function can be called again...
      updatePendingReturns(context.messageSender, (_ => 0))

      if (!Address.send(context.messageSender, amount)) {
        updatePendingReturns(context.messageSender, (_ => amount))
        false
      } else {
        true
      }
    } else {
      true
    }
  } ensuring (_ => requireInvariant(context))

  def endAuction()(context: Context): Unit = this.synchronized {
    // Can everybody end the auction?
    notBefore(auctionEndTime)(context) { () =>
      require(!ended, s"The auction has already ended")

      ended = true
      logger.info(s"Auction has ended. Highest bidder: ${highestBidderOption.getOrElse("")}. Highest bid: $highestBid")

      require(Address.send(beneficiary, highestBid), s"Could not send the highest bid to the beneficiary")
    }
  } ensuring (_ => requireInvariant(context))

  /**
   * Checks the contract instance invariant, throwing an exception if anything is not ok, and
   * returning true otherwise.
   */
  private[simulatesolidity] def requireInvariant(context: Context): Boolean = {
    assert(highestBid > 0, s"Only positive bids allowed")
    assert((highestBid == 0) || highestBidderOption.isDefined, s"A non-zero bid must have a bidder")

    assert(!ended || !Instant.now().isBefore(auctionEndTime), s"Cannot end the auction before the end time")

    true
  }

  private def updatePendingReturns(address: Address, f: Int => Int): Unit = {
    pendingReturns = Updater.updated(pendingReturns, address, f)
  }
}
