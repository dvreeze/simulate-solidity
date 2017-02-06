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

import scala.collection.immutable
import scala.util.Try

/**
 * Script combining several function calls. The "smart contracts" are implicit.
 *
 * @author Chris de Vreeze
 */
final class Script(
    val initialContext: FunctionCallContext,
    val functionCalls: immutable.IndexedSeq[FunctionCall]) {

  /**
   * Invokes all function calls "transactionally". See method callFunction.
   */
  def run(): HasAccountCollection = {
    val dummyFuncResult: HasAccountCollection = FunctionResult.fromCallContextOnly(initialContext)

    functionCalls.foldLeft(dummyFuncResult) {
      case (funcResult, funcCall) =>
        val newFuncResult = callFunction(funcCall, funcResult.accountCollection)
        newFuncResult
    }
  }

  /**
   * "Transactional" function call. It is somewhat ACID minus durability. It is atomic in that an exception
   * rolls back the state to the previous function result and therefore account collection. It is isolated
   * in that only one thread at a time can call this function on this script object. As a result, the
   * state (mainly account collection) goes from consistent state to consistent state.
   */
  def callFunction(
    functionCall: FunctionCall,
    accountCollection: AccountCollection): HasAccountCollection = this.synchronized {

    Try(functionCall(accountCollection)).toOption getOrElse {
      new FunctionResult((), accountCollection)
    }
  }

  /**
   * Like method callFunction, but returns the result wrapped in a Try, thus giving access to any thrown
   * exception.
   */
  def tryCallingFunction(
    functionCall: FunctionCall,
    accountCollection: AccountCollection): Try[HasAccountCollection] = this.synchronized {

    Try(functionCall(accountCollection))
  }
}
