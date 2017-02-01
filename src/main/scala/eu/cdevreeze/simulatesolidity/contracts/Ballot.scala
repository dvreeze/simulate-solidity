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

import scala.collection.immutable

import eu.cdevreeze.simulatesolidity.aspects.SenderAspects
import eu.cdevreeze.simulatesolidity.collections.Updater
import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.Contract
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionCallContext
import eu.cdevreeze.simulatesolidity.soliditytypes.FunctionResult

/**
 * Simulation of Ballot Solidity example.
 *
 * It often returns a boolean instead of throwing exceptions in random simulations of this contract,
 * except that the contract invariant quickly throws an exception if anything is wrong with the state
 * of the contract instance.
 *
 * See https://solidity.readthedocs.io/en/develop/solidity-by-example.html.
 *
 * The class is thread-safe.
 *
 * @author Chris de Vreeze
 */
final class Ballot(proposalNames: immutable.IndexedSeq[String])(val firstContext: FunctionCallContext, val ownAddress: Address) extends Contract with SenderAspects {
  require(proposalNames.nonEmpty, s"There must be at least one proposal")
  require(proposalNames.distinct.size == proposalNames.size, s"All proposals must have different names")

  // Proposals "table". It is mutable, but only the reference, whereas the snapshot itself is immutable.
  var proposals: immutable.IndexedSeq[Ballot.Proposal] = {
    proposalNames.map(nm => Ballot.Proposal(nm))
  }

  val chairPerson: Address = firstContext.messageSender

  // Voters "table". It is mutable, but only the reference, whereas the snapshot itself is immutable.
  var voters: Map[Address, Ballot.Voter] = {
    Map(chairPerson -> Ballot.Voter(chairPerson, 1))
  }

  requireInvariant(firstContext)

  /**
   * Gives the given voter the right to vote. Only callable by the chair person.
   */
  def giveRightToVote(voter: Address)(context: FunctionCallContext): FunctionResult[Unit] = this.synchronized {
    withRequiredSender(chairPerson)(context) { () =>
      if (voters.get(voter).forall(v => !v.voted)) {
        // Improvement over the original in order not to break the invariant: the weight never goes down.
        addOrUpdateVoter(voter, v => v.copy(weight = 1.max(v.weight)), addr => Ballot.Voter(addr))
      }

      FunctionResult.fromCallContextOnly(context)
    } ensuring { _ =>
      requireInvariant(context)
    }
  }

  /**
   * Delegates your vote to the given voter.
   */
  def delegate(to: Address)(context: FunctionCallContext): FunctionResult[Boolean] = this.synchronized {
    val sender = voters.getOrElse(context.messageSender, Ballot.Voter(context.messageSender))

    if (sender.voted) {
      FunctionResult.fromCallContextAndResult(context)(false)
    } else {
      val lastDelegate = getLastDelegate(voters.getOrElse(to, Ballot.Voter(to)))(context)
      require(lastDelegate.address != context.messageSender, s"Delegating to message sender ${context.messageSender} not allowed")
      require(!lastDelegate.votedByDelegation, s"The last delegate must not delegate himself or herself")

      addOrUpdateVoter(sender.address, _.copy(delegateOption = Some(lastDelegate.address)), _ => sender)

      // Improvement over the original in order not to break the invariant: updating the delegate weight also.
      addOrUpdateVoter(lastDelegate.address, v => v.copy(weight = v.weight + sender.weight), addr => Ballot.Voter(addr))

      // On the EVM, if an exception is thrown at this point, the changes to storage are rolled back to the point before this function call.

      // Improvement over the original in order not to break the invariant?
      if (lastDelegate.votedDirectly) {
        updateProposal(lastDelegate.votedProposalIndexOption.get, p => p.copy(voteCount = p.voteCount + sender.weight))
      }
      FunctionResult.fromCallContextAndResult(context)(true)
    } ensuring (_ => requireInvariant(context))
  }

  /**
   * Gives your vote including the ones delegated to you to the given proposal.
   * Returns true if the vote was successful, and false if the vote had already been made earlier.
   */
  def vote(proposalIdx: Int)(context: FunctionCallContext): FunctionResult[Boolean] = this.synchronized {
    require(proposalIdx >= 0 && proposalIdx < proposals.size, s"Proposal index $proposalIdx out of bounds")

    val voter = voters.getOrElse(context.messageSender, Ballot.Voter(context.messageSender))

    // Improvement over the original in order not to break the invariant: no voting if delegating.
    if (voter.voted) {
      FunctionResult.fromCallContextAndResult(context)(false)
    } else {
      addOrUpdateVoter(voter.address, _.vote(proposalIdx), _ => voter)

      // On the EVM, if an exception is thrown at this point, the changes to storage are rolled back to the point before this function call.

      updateProposal(proposalIdx, _.vote(voter.weight))
      FunctionResult.fromCallContextAndResult(context)(true)
    } ensuring (_ => requireInvariant(context))
  }

  def winningProposal: Ballot.Proposal = this.synchronized {
    // Sorting is expensive, of course.
    proposals.sortBy(_.voteCount).last
  }

  def winnerName: String = this.synchronized {
    winningProposal.name
  }

  private def getLastDelegate(voter: Ballot.Voter)(context: FunctionCallContext): Ballot.Voter = {
    if (voter.delegateOption.isEmpty || (voter.delegateOption == Some(context.messageSender))) {
      voter
    } else {
      // Recursive call
      getLastDelegate(voters(voter.delegateOption.get))(context)
    }
  }

  /**
   * Checks the contract instance invariant, throwing an exception if anything is not ok, and
   * returning true otherwise.
   *
   * The state of the contract instance is treated as a database snapshot, and the invariant
   * contains many database-like checks, such as referential integrity constraints.
   */
  private[simulatesolidity] def requireInvariant(context: FunctionCallContext): Boolean = {
    // Referential integrity, like in a database

    assert(voters.forall(kv => kv._2.address == kv._1), s"Corrupt data")
    assert(voters.values.flatMap(_.delegateOption).toSet.subsetOf(voters.keySet), s"Broken referential integrity")

    assert(voters.values.flatMap(_.votedProposalIndexOption).forall(idx => idx >= 0 && idx < proposals.size), s"Corrupt data")

    // Vote counts in total. This cannot be expressed in Solidity, because in Solidity we cannot iterate over mappings.

    val totalVoteCount = proposals.map(_.voteCount).sum

    val expectedVoteCount =
      voters.values.filter(_.votedProposalIndexOption.isDefined).map(_.weight).sum

    assert(expectedVoteCount == totalVoteCount, s"Vote count $totalVoteCount differs from the vote count by summing weights: $expectedVoteCount.")

    // Vote counts per proposal. This cannot be expressed in Solidity, because in Solidity we cannot iterate over mappings.

    val voteCountsByProposalName: Map[String, Int] =
      proposals.filter(_.voteCount != 0).map(proposal => (proposal.name -> proposal.voteCount)).toMap

    val expectedVoteCountsByProposalIndex: Map[Int, Int] =
      voters.values.filter(_.votedProposalIndexOption.isDefined).groupBy(_.votedProposalIndexOption.get) mapValues { voters =>
        voters.map(_.weight).sum
      } filter (_._2 != 0)

    val expectedVoteCountsByProposalName: Map[String, Int] =
      expectedVoteCountsByProposalIndex.toSeq.map({ case (proposalIdx, voteCount) => (proposals(proposalIdx).name -> voteCount) }).toMap

    assert(
      expectedVoteCountsByProposalName == voteCountsByProposalName,
      s"Vote count map $voteCountsByProposalName differs from the vote count map by summing weights: $expectedVoteCountsByProposalName.")

    true
  }

  private def updateProposal(proposalIdx: Int, f: Ballot.Proposal => Ballot.Proposal): Unit = {
    proposals = Updater.updated(proposals, proposalIdx, f)
  }

  private def addOrUpdateVoter(address: Address, f: Ballot.Voter => Ballot.Voter, mk: Address => Ballot.Voter): Unit = {
    voters = Updater.updated(voters, address, f, addr => mk(addr))
  }

  private def updateVoter(address: Address, f: Ballot.Voter => Ballot.Voter): Unit = {
    voters = Updater.updated(voters, address, f)
  }
}

object Ballot {

  /**
   * Voter. The weight increases on each delegation to this voter.
   *
   * Note that the address is kept as part of the state. Also note that property voted is derived instead
   * of a field, thus making it easier to reason about the state of the Voter object.
   *
   * Note that voted can be true even if no proposal was voted for! Just by doing this exercise
   * in Scala with only isolated mutability I found this out.
   */
  final case class Voter(
      val address: Address,
      val weight: Int = 0,
      val delegateOption: Option[Address] = None,
      val votedProposalIndexOption: Option[Int] = None) {

    require(
      delegateOption.isEmpty || votedProposalIndexOption.isEmpty,
      s"With delegation there can be no proposal that has been voted for")

    def votedDirectly: Boolean = votedProposalIndexOption.isDefined

    def votedByDelegation: Boolean = delegateOption.isDefined

    def voted: Boolean = votedDirectly || votedByDelegation

    def vote(newVotedProposalIndex: Int): Voter = {
      require(!voted, s"Cannot vote twice (or vote after delegation)")
      Voter(address, weight, delegateOption, Some(newVotedProposalIndex))
    }
  }

  final case class Proposal(val name: String, val voteCount: Int = 0) {
    require(voteCount >= 0, s"Vote count must not be negative")

    def vote(senderWeight: Int): Proposal = {
      require(senderWeight >= 0, s"Sender weight must not be negative")
      Proposal(name, voteCount + senderWeight)
    }
  }
}
