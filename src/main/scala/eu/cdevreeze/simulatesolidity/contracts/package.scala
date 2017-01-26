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

package eu.cdevreeze.simulatesolidity

/**
 * Several "smart contracts" simulated in Scala, in order to be able to better reason about correctness.
 *
 * Why use Scala to design contracts that must be implemented in a language like Solidity?
 *
 * First of all, with Scala it is possible to limit mutability as much as possible. Each program must
 * have some side-effects to be useful, but at least we can limit and isolate mutability as much as
 * possible, in order to make the attack surface area as small as possible, and to support local reasoning
 * about correctness.
 *
 * Local reasoning about correctness is a very desirable property in general, and essential for smart
 * contracts in a blockchain. After all, if you cannot reason locally about correctness, how can you
 * reason about correctness at all? Solidity is unfortunately not a very good language in this respect,
 * because of its "mutability everywhere" characteristic baked into the language.
 *
 * Very different in Solidity (compared to Scala/Java) is that mappings are considered fully filled for all
 * possible keys, mapping to "null objects" for each key for which no entry has been explicitly created!
 * You cannot even iterate over mappings in Solidity! In Scala, using Scala Maps, we do not have this
 * limitation. Moreover, the lack of Option types (and generics) in Solidity is a shortcoming, in my opinion.
 *
 * In Solidity we have to pay careful attention to the difference between storage and memory. Using the JVM
 * heap memory is more straightforward, in comparison. Of course some persistent storage is needed, but why
 * having to worry about different kinds of memory in every line of code?
 *
 * It is also instructive to compare storage in Solidity with database tables, and see how Solidity does or
 * does not help in enforcing "database constraints".
 *
 * See https://solidity.readthedocs.io/en/develop/solidity-by-example.html for several non-trivial Solidity examples.
 *
 * The Scala examples simulate storage variables by public fields of the contract instance. After all, these
 * fields are meant to be persistent across public function calls.
 *
 * Here I did not worry about used gas. First of all, it has no meaning in this simulation. Second,
 * it is "obviously correct code" that I am after, as the high-level design of a contract.
 *
 * @author Chris de Vreeze
 */
package object contracts
