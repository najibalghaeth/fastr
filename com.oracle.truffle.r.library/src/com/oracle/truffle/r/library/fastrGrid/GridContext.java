/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.library.fastrGrid.WindowDevice.awtNotSupported;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.GridState.GridDeviceState;
import com.oracle.truffle.r.library.fastrGrid.device.FileGridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice.DeviceCloseException;
import com.oracle.truffle.r.library.fastrGrid.device.SVGDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice.NotSupportedImageFormatException;
import com.oracle.truffle.r.library.fastrGrid.grDevices.FileDevUtils;
import com.oracle.truffle.r.library.fastrGrid.graphics.RGridGraphicsAdapter;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Encapsulated the access to the global grid state.
 */
public final class GridContext {
    private RInternalCode internalCode;
    private final GridState gridState = new GridState();
    /**
     * This list should correspond to the names inside {@code .Devices} variable in R.
     */
    private final ArrayList<DeviceAndState> devices = new ArrayList<>(2);
    private int currentDeviceIdx = 0;

    private GridContext() {
        devices.add(new DeviceAndState(null, null));
    }

    public static GridContext getContext() {
        RContext rCtx = RContext.getInstance();
        if (rCtx.gridContext == null) {
            rCtx.gridContext = new GridContext();
        }
        return (GridContext) rCtx.gridContext;
    }

    @TruffleBoundary
    public GridState getGridState() {
        gridState.setDeviceState(devices.get(currentDeviceIdx).state);
        return gridState;
    }

    public int getCurrentDeviceIndex() {
        return currentDeviceIdx;
    }

    public int getDevicesSize() {
        return devices.size();
    }

    public GridDevice getCurrentDevice() {
        assert currentDeviceIdx >= 0 : "accessing devices before they were initialized";
        return devices.get(currentDeviceIdx).device;
    }

    public void setCurrentDevice(String name, GridDevice currentDevice) {
        assert !(currentDevice instanceof FileGridDevice) : "FileGridDevice must have filenamePattern";
        setCurrentDevice(name, currentDevice, null);
    }

    public void setCurrentDevice(String name, GridDevice currentDevice, String filenamePattern) {
        RGridGraphicsAdapter.addDevice(name);
        RGridGraphicsAdapter.setCurrentDevice(name);
        currentDeviceIdx = this.devices.size();
        this.devices.add(new DeviceAndState(currentDevice, filenamePattern));
        assert devices.size() == RGridGraphicsAdapter.getDevicesCount();
    }

    public void openDefaultDevice() {
        String defaultDev = RGridGraphicsAdapter.getDefaultDevice();
        if (defaultDev.equals("awt") || defaultDev.startsWith("X11")) {
            if (!FastRConfig.InternalGridAwtSupport) {
                throw awtNotSupported();
            }
            setCurrentDevice(defaultDev, WindowDevice.createWindowDevice());
        } else if (defaultDev.equals("svg")) {
            String filename = "Rplot%03d.svg";
            SVGDevice svgDevice = new SVGDevice(FileDevUtils.formatInitialFilename(filename), GridDevice.DEFAULT_WIDTH, GridDevice.DEFAULT_HEIGHT);
            setCurrentDevice(defaultDev, svgDevice, filename);
        } else if (defaultDev.equals("png")) {
            safeOpenImageDev("Rplot%03d.png", "png");
        } else if (defaultDev.equals("bmp")) {
            safeOpenImageDev("Rplot%03d.bmp", "bmp");
        } else if (defaultDev.equals("jpg") || defaultDev.equals("jpeg")) {
            safeOpenImageDev("Rplot%03d.jpg", "jpeg");
        } else {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "FastR does not support device '" + defaultDev + "'.");
        }
        assert devices.size() == RGridGraphicsAdapter.getDevicesCount();
    }

    public void closeDevice(int which) throws DeviceCloseException {
        assert which >= 0 && which < devices.size();
        devices.get(which).device.close();
        removeDevice(which);
    }

    public void removeDevice(int which) {
        RGridGraphicsAdapter.removeDevice(which);
        devices.remove(which);
        if (currentDeviceIdx >= which) {
            currentDeviceIdx--;
        }
    }

    public GridDevice getDevice(int index) {
        return devices.get(index).device;
    }

    /**
     * Runs arbitrary function from 'fastrGrid.R' file and returns its result.
     */
    public Object evalInternalRFunction(String functionName, Object... args) {
        if (internalCode == null) {
            internalCode = RInternalCode.lookup(RContext.getInstance(), "grid", RInternalCode.loadSourceRelativeTo(LInitGrid.class, "fastrGrid.R"));
        }
        RFunction redrawAll = internalCode.lookupFunction(functionName);
        return RContext.getEngine().evalFunction(redrawAll, REnvironment.baseEnv().getFrame(), RCaller.topLevel, true, null, args);
    }

    private void safeOpenImageDev(String filename, String formatName) {
        if (!FastRConfig.InternalGridAwtSupport) {
            throw awtNotSupported();
        }
        BufferedImageDevice dev = null;
        try {
            dev = BufferedImageDevice.open(FileDevUtils.formatInitialFilename(filename), formatName, GridDevice.DEFAULT_WIDTH, GridDevice.DEFAULT_HEIGHT);
        } catch (NotSupportedImageFormatException e) {
            throw RInternalError.shouldNotReachHere("Device format " + formatName + " should be supported.");
        }
        setCurrentDevice(formatName, dev, filename);
    }

    private static final class DeviceAndState {
        final GridDevice device;
        final GridDeviceState state;

        DeviceAndState(GridDevice device, String filenamePattern) {
            this.device = device;
            this.state = new GridDeviceState(filenamePattern);
        }
    }
}
