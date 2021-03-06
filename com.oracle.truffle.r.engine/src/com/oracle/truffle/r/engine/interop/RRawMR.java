/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RRaw;

@MessageResolution(receiverType = RRaw.class)
public class RRawMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RRawIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RRaw receiver) {
            return true;
        }
    }

    @Resolve(message = "UNBOX")
    public abstract static class RRawUnboxNode extends Node {
        protected Object access(@SuppressWarnings("unused") RRaw receiver) {
            return receiver.getValue();
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RRawHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RRaw receiver) {
            return false;
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RRawKeyInfoNode extends Node {
        protected Object access(@SuppressWarnings("unused") RRaw receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }

    @CanResolve
    public abstract static class RComplexCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RRaw;
        }
    }
}
