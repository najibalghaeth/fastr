/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Temporary substitutions that just evaluate the expression for package loading and assume no
 * errors or finally statements.
 */
public class TryFunctions {
    public abstract static class Adapter extends RBuiltinNode {
        @Child private PromiseHelperNode promiseHelper;

        protected PromiseHelperNode initPromiseHelper() {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper;
        }
    }

    @RBuiltin(name = "try", kind = RBuiltinKind.SUBSTITUTE, parameterNames = {"expr", "silent"}, nonEvalArgs = {0})
    public abstract static class Try extends Adapter {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        protected Object doTry(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") byte silent) {
            controlVisibility();
            return initPromiseHelper().evaluate(frame, expr);
        }
    }

    // Ignoring finally completely
    @RBuiltin(name = "tryCatch", kind = RBuiltinKind.SUBSTITUTE, parameterNames = {"expr", "..."}, nonEvalArgs = {-1})
    public abstract static class TryCatch extends Adapter {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY)};
        }

        @Specialization
        protected Object doTryCatch(VirtualFrame frame, RPromise expr, RPromise arg) {
            return doTryCatch(frame, expr, new RArgsValuesAndNames(new Object[]{arg}, ArgumentsSignature.empty(1)));
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doTryCatch(VirtualFrame frame, RPromise expr, RArgsValuesAndNames args) {
            controlVisibility();
            return initPromiseHelper().evaluate(frame, expr);
        }
    }

    // A temporary substitute that does not suppress warnings!
    @RBuiltin(name = "suppressWarnings", kind = RBuiltinKind.SUBSTITUTE, parameterNames = {"expr"}, nonEvalArgs = {0})
    public abstract static class SuppressWarningsBuiltin extends Adapter {
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected Object doSuppressWarnings(VirtualFrame frame, RPromise expr) {
            controlVisibility();
            return initPromiseHelper().evaluate(frame, expr);
        }

    }
}
