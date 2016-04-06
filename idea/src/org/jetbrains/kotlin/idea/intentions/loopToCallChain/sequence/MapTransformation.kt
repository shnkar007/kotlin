/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtProperty

class MapTransformation(
        override val loop: KtForExpression,
        override val inputVariable: KtCallableDeclaration,
        val mapping: KtExpression
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = false

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, mapping)
        return chainedCallGenerator.generate("map$0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         val v = ...
     *         ...
     *     }
     */
    object Matcher : SequenceTransformationMatcher {
        override fun match(state: MatchingState): SequenceTransformationMatch? {
            if (state.indexVariable != null) return null //TODO?

            val declaration = state.statements.firstOrNull() as? KtProperty ?: return null //TODO: support multi-variables
            val initializer = declaration.initializer ?: return null
            if (declaration.hasWriteUsages()) return null
            val restStatements = state.statements.drop(1)

            val transformation = MapTransformation(state.outerLoop, state.inputVariable, initializer)
            val newState = state.copy(statements = restStatements, inputVariable = declaration)
            return SequenceTransformationMatch(transformation, newState)
        }
    }
}