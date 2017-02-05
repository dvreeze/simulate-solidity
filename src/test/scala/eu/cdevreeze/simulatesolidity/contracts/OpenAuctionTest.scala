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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.simulatesolidity.collections._
import eu.cdevreeze.simulatesolidity.soliditytypes._

/**
 * Open auction test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class OpenAuctionTest extends FunSuite {

  private val addressCount = 10

  private def makeInitialContext(initialSender: Address): FunctionCallContext = {
    val accountMap = (1 to addressCount).map(i => (Address(i) -> getAccount(Address(i)))).toMap

    val accountCollection = new AccountCollection(accountMap)

    val message = Message.withoutWei(initialSender)

    new FunctionCallContext(message, accountCollection)
  }

  private def getAccount(addr: Address): Account = {
    if (addr.addressValue == 2) {
      ContractAccount(addr, 0)
    } else {
      ExternalAccount(addr, 100)
    }
  }

  private val initialContext = makeInitialContext(Address(1))

  private val beneficiary = Address(1)

  test("testAuction") {
    val durationInSec = 60
    val auction: OpenAuction = new OpenAuction(beneficiary, durationInSec)(initialContext, Address(2))

    // Note how sensitive this all is. If user 4 withdrew before the highest bid by 6, the withdrawal
    // would not be successful. More in general, reasoning about sending WEI with smart contract functions
    // defeats local reasoning, and is quite hard to get right in detail. Functions that operate only
    // on the storage (and memory) of the contract itself tend to be much easier to reason about,
    // because they support local reasoning.

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall(
            Message(Address(3), 40),
            auction.ownAddress,
            auction.bid()),
          FunctionCall(
            Message(Address(4), 70),
            auction.ownAddress,
            auction.bid()),
          FunctionCall(
            Message(Address(5), 50),
            auction.ownAddress,
            auction.bid()),
          FunctionCall.withoutWei(
            Address(3),
            auction.ownAddress,
            auction.withdraw()),
          FunctionCall(
            Message(Address(6), 90),
            auction.ownAddress,
            auction.bid()),
          FunctionCall.withoutWei(
            Address(4),
            auction.ownAddress,
            auction.withdraw()),
          FunctionCall(
            Message(Address(7), 70),
            auction.ownAddress,
            auction.bid())))

    val lastFuncResult = script.run()

    assertResult(90) {
      auction.highestBid
    }
    assertResult(Some(Address(6))) {
      auction.highestBidderOption
    }

    val withdrawals = Set(Address(3), Address(4), Address(5), Address(7))

    assertResult(initialContext.accountCollection.accountsByAddress.filterKeys(withdrawals)) {
      lastFuncResult.accountCollection.accountsByAddress.filterKeys(withdrawals)
    }

    assertResult(initialContext.accountCollection.accountsByAddress(Address(6)).subtractAmount(90)) {
      lastFuncResult.accountCollection.accountsByAddress(Address(6))
    }
  }
}
