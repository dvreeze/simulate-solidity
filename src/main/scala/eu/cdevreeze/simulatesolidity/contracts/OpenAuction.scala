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
import eu.cdevreeze.simulatesolidity.soliditytypes.Contract
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionCallContext
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionResult

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
    val biddingDurationInSeconds: Int)(val firstContext: FunctionCallContext, val ownAddress: Address) extends Contract with SenderAspects with TimeAspects {

  private val logger: Logger = Logger.getGlobal

  val auctionStartTime: Instant = Instant.now()

  private val auctionEndTime: Instant = {
    auctionStartTime.plusSeconds(biddingDurationInSeconds)
  }

  var highestBidderOption: Option[Address] = None

  var highestBid: BigInt = 0

  var pendingReturns: Map[Address, BigInt] = Map()

  var ended: Boolean = false

  /**
   * Bid on the auction with the value sent with the transaction. This function is "payable".
   */
  def bid()(context: FunctionCallContext): FunctionResult[Unit] = this.synchronized {
    // Improvement?
    withSenderOtherThan(highestBidderOption.toSet.union(pendingReturns.keySet))(context) { () =>
      notAfter(auctionEndTime)(context) { () =>
        require(context.message.messageValueInWei > highestBid, s"Bid below highest bid. Does a refund.")

        if (highestBidderOption.isDefined) {
          assert(highestBidderOption.get != context.messageSender)

          // Pull instead of push money transfer...
          updatePendingReturns(highestBidderOption.get, (_ + highestBid))
        }

        highestBidderOption = Some(context.messageSender)
        highestBid = context.message.messageValueInWei // The calling infrastructure must pay this amount to the contract address!

        logger.info(s"Highest bid increased. Sender: ${context.messageSender}. Value: ${highestBid}")
      }
    }

    FunctionResult.fromCallContextOnly(context)
  } ensuring (_ => requireInvariant(context))

  def withdraw()(context: FunctionCallContext): FunctionResult[Boolean] = this.synchronized {
    val amount: BigInt = pendingReturns.getOrElse(context.messageSender, 0)

    if (amount > 0) {
      // This function can be called again...
      updatePendingReturns(context.messageSender, (_ => 0))

      val newContextOption = context.send(ownAddress, context.messageSender, amount)

      if (newContextOption.isEmpty) {
        updatePendingReturns(context.messageSender, (_ => amount))
        FunctionResult.fromCallContextAndResult(context)(false)
      } else {
        FunctionResult.fromCallContextAndResult(newContextOption.get)(true)
      }
    } else {
      FunctionResult.fromCallContextAndResult(context)(true)
    }
  } ensuring (_ => requireInvariant(context))

  def endAuction()(context: FunctionCallContext): FunctionResult[Unit] = this.synchronized {
    // Can everybody end the auction?
    notBefore(auctionEndTime)(context) { () =>
      require(!ended, s"The auction has already ended")

      ended = true
      logger.info(s"Auction has ended. Highest bidder: ${highestBidderOption.getOrElse("")}. Highest bid: $highestBid")

      val newContextOption = context.send(ownAddress, beneficiary, highestBid)
      require(newContextOption.isDefined, s"Could not send the highest bid to the beneficiary")

      FunctionResult.fromCallContextOnly(newContextOption.getOrElse(context))
    }
  } ensuring (_ => requireInvariant(context))

  /**
   * Checks the contract instance invariant, throwing an exception if anything is not ok, and
   * returning true otherwise.
   */
  private[simulatesolidity] def requireInvariant(context: FunctionCallContext): Boolean = {
    assert(highestBid > 0, s"Only positive bids allowed")
    assert((highestBid == 0) || highestBidderOption.isDefined, s"A non-zero bid must have a bidder")

    assert(!ended || !Instant.now().isBefore(auctionEndTime), s"Cannot end the auction before the end time")

    assert(
      highestBidderOption.forall(bidder => !pendingReturns.contains(bidder)),
      s"Cannot be highest bidder and have pending returns at the same time")

    true
  }

  private def updatePendingReturns(address: Address, f: BigInt => BigInt): Unit = {
    pendingReturns = Updater.updated(pendingReturns, address, f)
  }
}
