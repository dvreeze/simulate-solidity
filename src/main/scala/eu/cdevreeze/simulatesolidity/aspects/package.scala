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
 * For each "contract" and each of its methods we need to think about WHO may call the method, WHEN
 * it may be called (in time or contract states), HOW OFTEN it may be called (or HOW OFTEN PER SENDER),
 * etc. Invariants are also important, and should be used in each non-trivial contract.
 *
 * Another important design decision is whether there are some well-defined phases in the contract.
 *
 * This package offers some support for some of the above-mentioned aspects.
 *
 * @author Chris de Vreeze
 */
package object aspects
