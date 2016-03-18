/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class ConstantNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxConstant, VisibilityController {

    private ConstantNode(SourceSection sourceSection) {
        super(sourceSection);
    }

    public static boolean isFunction(RNode node) {
        return node instanceof ConstantObjectNode && ((ConstantObjectNode) node).value instanceof RFunction;
    }

    public static boolean isMissing(RNode node) {
        return node instanceof ConstantObjectNode && ((ConstantObjectNode) node).value == RMissing.instance;
    }

    public static String getString(RSyntaxNode node) {
        if (node instanceof ConstantObjectNode) {
            Object value = ((ConstantObjectNode) node).value;
            return RRuntime.asString(value);
        }
        return null;
    }

    @Override
    public abstract Object getValue();

    @Override
    public final Object execute(VirtualFrame frame) {
        controlVisibility();
        return getValue();
    }

    @Override
    @TruffleBoundary
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        RDeparse.deparse2buff(state, getValue());
        state.endNodeDeparse(this);
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return this;
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setCar(getValue());
    }

    public static ConstantNode create(Object value) {
        return create(RSyntaxNode.SOURCE_UNAVAILABLE, value);
    }

    public static ConstantNode create(SourceSection sourceSection, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (value instanceof Integer) {
            return new ConstantIntegerScalarNode(sourceSection, (Integer) value);
        } else if (value instanceof Double) {
            return new ConstantDoubleScalarNode(sourceSection, (Double) value);
        } else if (value instanceof Byte) {
            return new ConstantLogicalScalarNode(sourceSection, (Byte) value);
        } else if (value instanceof String) {
            return new ConstantObjectNode(sourceSection, value);
        } else if (value instanceof RSymbol) {
            return new ConstantObjectNode(sourceSection, value);
        } else if (value instanceof RArgsValuesAndNames) {
            // this can be created during argument matching and "call"
            return new ConstantObjectNode(sourceSection, value);
        } else {
            assert value instanceof RTypedValue && !(value instanceof RPromise) : value;
            return new ConstantObjectNode(sourceSection, value);
        }
    }

    private static final class ConstantDoubleScalarNode extends ConstantNode {

        private final Double objectValue;
        private final double doubleValue;

        ConstantDoubleScalarNode(SourceSection sourceSection, double value) {
            super(sourceSection);
            this.objectValue = value;
            this.doubleValue = value;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            controlVisibility();
            return doubleValue;
        }
    }

    private static final class ConstantLogicalScalarNode extends ConstantNode {

        private final byte logicalValue;
        private final Byte objectValue;

        ConstantLogicalScalarNode(SourceSection sourceSection, byte value) {
            super(sourceSection);
            this.logicalValue = value;
            this.objectValue = value;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }

        @Override
        public byte executeByte(VirtualFrame frame) {
            controlVisibility();
            return logicalValue;
        }
    }

    private static final class ConstantIntegerScalarNode extends ConstantNode {

        private final Integer objectValue;
        private final int intValue;

        ConstantIntegerScalarNode(SourceSection sourceSection, int value) {
            super(sourceSection);
            this.objectValue = value;
            this.intValue = value;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }

        @Override
        public int executeInteger(VirtualFrame frame) {
            controlVisibility();
            return intValue;
        }
    }

    private static final class ConstantObjectNode extends ConstantNode {

        private final Object value;

        ConstantObjectNode(SourceSection sourceSection, Object value) {
            super(sourceSection);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        @TruffleBoundary
        public void deparseImpl(RDeparse.State state) {
            if (value == RMissing.instance || value instanceof RArgsValuesAndNames) {
                state.startNodeDeparse(this);
                if (value == RMissing.instance) {
                    // nothing to do
                } else if (value instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames args = (RArgsValuesAndNames) value;
                    Object[] values = args.getArguments();
                    for (int i = 0; i < values.length; i++) {
                        String name = args.getSignature().getName(i);
                        if (name != null) {
                            state.append(name);
                            state.append(" = ");
                        }
                        Object argValue = values[i];
                        if (argValue instanceof RSyntaxNode) {
                            ((RSyntaxNode) argValue).deparseImpl(state);
                        } else if (argValue instanceof RPromise) {
                            RASTUtils.unwrap(((RPromise) argValue).getRep()).deparse(state);
                        } else {
                            RInternalError.shouldNotReachHere();
                        }
                        if (i < values.length - 1) {
                            state.append(", ");
                        }
                    }
                }
                state.endNodeDeparse(this);
            } else {
                super.deparseImpl(state);
            }
        }

        @Override
        public void serializeImpl(RSerialize.State state) {
            if (value == RMissing.instance) {
                state.setCar(RMissing.instance);
            } else {
                super.serializeImpl(state);
            }
        }
    }

    public static Integer asIntConstant(RSyntaxNode argument, boolean castFromDouble) {
        if (argument instanceof ConstantNode) {
            Object value = ((ConstantNode) argument).getValue();
            if (value instanceof Integer) {
                return (int) value;
            } else if (castFromDouble && value instanceof Double) {
                return (int) (double) value;
            }
        }
        return null;
    }
}
