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
 * Schema content test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BallotTest extends FunSuite {

  private val addressCount = 10

  private def makeInitialContext(initialSender: Address): FunctionCallContext = {
    val accountMap = (1 to addressCount).map(i => (Address(i) -> getAccount(Address(i)))).toMap

    val accountCollection = new AccountCollection(accountMap)

    val message = Message.withoutWei(initialSender)

    new FunctionCallContext(message, accountCollection)
  }

  private def getAccount(addr: Address): Account = {
    if (addr.addressValue == 1) {
      ContractAccount(addr, 0)
    } else {
      ExternalAccount(addr, 0)
    }
  }

  private val initialContext = makeInitialContext(Address(1))

  private val proposalNames = Vector("dem", "rep", "ronpaul")

  test("testGiveRightToVote") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    val ctx = initialContext.withMessage(Message.withoutWei(initialContext.messageSender))
    ballot.requireInvariant(ctx)

    val funcResult = ballot.giveRightToVote(Address(3))(ctx)

    assertResult(Set(1)) {
      ballot.voters.filterKeys(Set(Address(2), Address(3))).map(_._2.weight).toSet
    }
    assertResult(true) {
      ballot.voters.filterKeys(_.addressValue >= 4).map(_._2.weight).toSet.subsetOf(Set(0))
    }

    assertResult(ctx.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testGiveRightToVoteTwice") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    val ctx1 = initialContext.withMessage(Message.withoutWei(initialContext.messageSender))
    ballot.requireInvariant(ctx1)

    val funcResult1 = ballot.giveRightToVote(Address(3))(ctx1)

    val ctx2 = FunctionCallContext(Message.withoutWei(initialContext.messageSender), funcResult1.accountCollection)
    val funcResult2 = ballot.giveRightToVote(Address(3))(ctx2)

    // The second call makes no difference
    assertResult(Set(1)) {
      ballot.voters.filterKeys(Set(Address(2), Address(3))).map(_._2.weight).toSet
    }
    assertResult(true) {
      ballot.voters.filterKeys(_.addressValue >= 4).map(_._2.weight).toSet.subsetOf(Set(0))
    }

    assertResult(ctx1.accountCollection) {
      funcResult2.accountCollection
    }
  }

  test("testGiveRightToVoteIfNotAllowed") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    val ctx = initialContext.withMessage(Message.withoutWei(Address(5)))
    ballot.requireInvariant(ctx)

    assertThrows[Exception] {
      ballot.giveRightToVote(Address(3))(ctx)
    }
  }

  test("testGiveRightToVoteIfAlreadyVoted") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1, None, Some(2)))
    ballot.proposals = Updater.updated(ballot.proposals, 2, ((p: Ballot.Proposal) => p.vote(1)))

    val ctx = initialContext.withMessage(Message.withoutWei(initialContext.messageSender))
    ballot.requireInvariant(ctx)

    assertThrows[Exception] {
      ballot.giveRightToVote(Address(3))(ctx)
    }
  }

  test("testGiveRightToVoteIfAlreadyDelegated") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters =
      ballot.voters +
        (Address(3) -> Ballot.Voter(Address(3), 1, Some(Address(5)), None)) +
        (Address(5) -> Ballot.Voter(Address(5), 1, None, None))

    val ctx = initialContext.withMessage(Message.withoutWei(initialContext.messageSender))
    ballot.requireInvariant(ctx)

    assertThrows[Exception] {
      ballot.giveRightToVote(Address(3))(ctx)
    }
  }

  test("testDelegate") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1))

    val sender = Address(3)
    val ctx = initialContext.withMessage(Message.withoutWei(sender))
    ballot.requireInvariant(ctx)

    val funcResult = ballot.delegate(Address(5))(ctx)

    assertResult(1) {
      ballot.voters(sender).weight
    }
    assertResult(1) {
      ballot.voters(Address(5)).weight
    }

    assertResult(true) {
      ballot.voters(sender).votedByDelegation
    }
    assertResult(false) {
      ballot.voters(Address(5)).voted
    }

    assertResult(ctx.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testDelegateIfDelegateVoted") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters =
      ballot.voters +
        (Address(3) -> Ballot.Voter(Address(3), 1)) +
        (Address(5) -> Ballot.Voter(Address(5), 4, None, Some(2)))
    ballot.proposals = Updater.updated(ballot.proposals, 2, ((p: Ballot.Proposal) => p.vote(4)))

    val sender = Address(3)
    val ctx = initialContext.withMessage(Message.withoutWei(sender))
    ballot.requireInvariant(ctx)

    val funcResult = ballot.delegate(Address(5))(ctx)

    assertResult(1) {
      ballot.voters(sender).weight
    }
    assertResult(5) {
      ballot.voters(Address(5)).weight
    }

    assertResult(true) {
      ballot.voters(sender).votedByDelegation
    }
    assertResult(true) {
      ballot.voters(Address(5)).voted
    }

    assertResult(ctx.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testDelegateTwice") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1))

    val sender = Address(3)
    val ctx1 = initialContext.withMessage(Message.withoutWei(sender))
    ballot.requireInvariant(ctx1)

    val funcResult1 = ballot.delegate(Address(5))(ctx1)

    val ctx2 = FunctionCallContext(Message.withoutWei(sender), funcResult1.accountCollection)

    assertThrows[Exception] {
      ballot.delegate(Address(5))(ctx2)
    }
  }

  test("testVote") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 5))

    val sender = Address(3)
    val ctx = initialContext.withMessage(Message.withoutWei(sender))
    ballot.requireInvariant(ctx)

    val funcResult = ballot.vote(2)(ctx)

    assertResult(true) {
      ballot.voters(sender).votedDirectly
    }
    assertResult(false) {
      ballot.voters(sender).votedByDelegation
    }

    assertResult(ctx.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testMultipleVotes") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.giveRightToVote(Address(3))),
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.giveRightToVote(Address(4))),
          FunctionCall.withoutWei(
            Address(4),
            ballot.delegate(Address(5))),
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.giveRightToVote(Address(6))),
          FunctionCall.withoutWei(
            Address(3),
            ballot.vote(2)),
          FunctionCall.withoutWei(
            Address(5),
            ballot.vote(2)),
          FunctionCall.withoutWei(
            Address(6),
            ballot.vote(2))))

    val lastFuncResult = script.run()

    assertResult(initialContext.accountCollection) {
      lastFuncResult.accountCollection
    }
    assertResult(3) {
      ballot.proposals(2).voteCount
    }
  }
}
