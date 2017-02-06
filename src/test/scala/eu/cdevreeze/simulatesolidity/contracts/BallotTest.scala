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

import eu.cdevreeze.simulatesolidity.collections.Updater
import eu.cdevreeze.simulatesolidity.soliditytypes.Account
import eu.cdevreeze.simulatesolidity.soliditytypes.AccountCollection
import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.ContractAccount
import eu.cdevreeze.simulatesolidity.soliditytypes.ExternalAccount
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionCall
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionCallContext
import eu.cdevreeze.simulatesolidity.soliditytypes.Message
import eu.cdevreeze.simulatesolidity.soliditytypes.Script

/**
 * Ballot test case.
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
    if (addr.addressValue == 2) {
      ContractAccount(addr, 0)
    } else {
      ExternalAccount(addr, 0)
    }
  }

  private val initialContext = makeInitialContext(Address(1))

  private val proposalNames = Vector("dem", "rep", "ronpaul")

  test("testGiveRightToVote") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3)))))

    val funcResult = script.run()

    assertResult(Set(1)) {
      ballot.voters.filterKeys(Set(Address(2), Address(3))).map(_._2.weight).toSet
    }
    assertResult(true) {
      ballot.voters.filterKeys(_.addressValue >= 4).map(_._2.weight).toSet.subsetOf(Set(0))
    }

    assertResult(initialContext.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testGiveRightToVoteTwice") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3))),
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3)))))

    val funcResult = script.run()

    // The second call makes no difference
    assertResult(Set(1)) {
      ballot.voters.filterKeys(Set(Address(2), Address(3))).map(_._2.weight).toSet
    }
    assertResult(true) {
      ballot.voters.filterKeys(_.addressValue >= 4).map(_._2.weight).toSet.subsetOf(Set(0))
    }

    assertResult(initialContext.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testGiveRightToVoteIfNotAllowed") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            Address(5),
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3)))))

    assertResult(None) {
      script.tryCallingFunction(script.functionCalls.head, script.initialContext.accountCollection).toOption
    }
  }

  test("testGiveRightToVoteIfAlreadyVoted") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1, None, Some(2)))
    ballot.proposals = Updater.updated(ballot.proposals, 2, ((p: Ballot.Proposal) => p.vote(1)))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3)))))

    assertResult(None) {
      script.tryCallingFunction(script.functionCalls.head, script.initialContext.accountCollection).toOption
    }
  }

  test("testGiveRightToVoteIfAlreadyDelegated") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters =
      ballot.voters +
        (Address(3) -> Ballot.Voter(Address(3), 1, Some(Address(5)), None)) +
        (Address(5) -> Ballot.Voter(Address(5), 1, None, None))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3)))))

    assertResult(None) {
      script.tryCallingFunction(script.functionCalls.head, script.initialContext.accountCollection).toOption
    }
  }

  test("testDelegate") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1))

    val sender = Address(3)
    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            sender,
            ballot.ownAddress,
            ballot.delegate(Address(5)))))

    val funcResult = script.run()

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

    assertResult(initialContext.accountCollection) {
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
    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            sender,
            ballot.ownAddress,
            ballot.delegate(Address(5)))))

    val funcResult = script.run()

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

    assertResult(initialContext.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testDelegateTwice") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 1))

    val sender = Address(3)
    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            sender,
            ballot.ownAddress,
            ballot.delegate(Address(5))),
          FunctionCall.withoutWei(
            sender,
            ballot.ownAddress,
            ballot.delegate(Address(5)))))

    val funcResult = script.run()

    assertResult(true) {
      ballot.voters(sender).votedByDelegation
    }
  }

  test("testDelegateRepeatedly") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters =
      ballot.voters +
        (Address(3) -> Ballot.Voter(Address(3), 1)) +
        (Address(6) -> Ballot.Voter(Address(6), 2))

    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            Address(3),
            ballot.ownAddress,
            ballot.delegate(Address(5))),
          FunctionCall.withoutWei(
            Address(5),
            ballot.ownAddress,
            ballot.delegate(Address(6))),
          FunctionCall.withoutWei(
            Address(6),
            ballot.ownAddress,
            ballot.delegate(Address(7)))))

    val funcResult = script.run()

    assertResult(1) {
      ballot.voters(Address(3)).weight
    }
    assertResult(1) {
      ballot.voters(Address(5)).weight
    }
    assertResult(3) {
      ballot.voters(Address(6)).weight
    }
    assertResult(3) {
      ballot.voters(Address(7)).weight
    }

    assertResult(true) {
      ballot.voters(Address(3)).votedByDelegation
    }
    assertResult(true) {
      ballot.voters(Address(5)).votedByDelegation
    }
    assertResult(true) {
      ballot.voters(Address(6)).votedByDelegation
    }
    assertResult(false) {
      ballot.voters(Address(7)).voted
    }

    assertResult(initialContext.accountCollection) {
      funcResult.accountCollection
    }
  }

  test("testVote") {
    val ballot: Ballot = new Ballot(proposalNames)(initialContext, Address(2))
    ballot.voters = ballot.voters + (Address(3) -> Ballot.Voter(Address(3), 5))

    val sender = Address(3)
    ballot.requireInvariant(initialContext)

    val script =
      new Script(
        initialContext,
        Vector[FunctionCall](
          FunctionCall.withoutWei(
            sender,
            ballot.ownAddress,
            ballot.vote(2))))

    val funcResult = script.run()

    assertResult(true) {
      ballot.voters(sender).votedDirectly
    }
    assertResult(false) {
      ballot.voters(sender).votedByDelegation
    }

    assertResult(initialContext.accountCollection) {
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
            ballot.ownAddress,
            ballot.giveRightToVote(Address(3))),
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(4))),
          FunctionCall.withoutWei(
            Address(4),
            ballot.ownAddress,
            ballot.delegate(Address(5))),
          FunctionCall.withoutWei(
            ballot.chairPerson,
            ballot.ownAddress,
            ballot.giveRightToVote(Address(6))),
          FunctionCall.withoutWei(
            Address(3),
            ballot.ownAddress,
            ballot.vote(2)),
          FunctionCall.withoutWei(
            Address(5),
            ballot.ownAddress,
            ballot.vote(2)),
          FunctionCall.withoutWei(
            Address(6),
            ballot.ownAddress,
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
