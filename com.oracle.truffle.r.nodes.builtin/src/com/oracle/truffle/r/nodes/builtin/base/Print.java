/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "print.default", kind = INTERNAL, parameterNames = {"x", "digits", "quote", "na.print", "print.gap", "right", "max", "useSource", "noOpt"})
// TODO revert to R
public abstract class Print extends RInvisibleBuiltinNode {

    @CompilationFinal private static final RNode[] PARAMETER_VALUES = new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance),
                    ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance),
                    ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};

    @Child private PrettyPrinterNode prettyPrinter = PrettyPrinterNodeFactory.create(null, null, null, null, false);

    private static void printHelper(String string) {
        RContext.getInstance().getConsoleHandler().println(string);
    }

    @Override
    public RNode[] getParameterValues() {
        return PARAMETER_VALUES;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object print(VirtualFrame frame, Object o, Object digits, byte quote, Object naPrint, Object printGap, byte right, Object max, Object useSource, Object noOpt) {
        String s = (String) prettyPrinter.executeString(frame, o, null, quote, right);
        if (s != null && !s.isEmpty()) {
            printHelper(s);
        }
        controlVisibility();
        return o;
    }
}
