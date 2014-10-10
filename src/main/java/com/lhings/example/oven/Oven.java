/* Copyright 2014 Lyncos Technologies S. L.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.lhings.example.oven;

import com.lhings.java.LhingsDevice;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.lhings.java.annotations.Action;
import com.lhings.java.annotations.Event;
import com.lhings.java.annotations.StatusComponent;
import com.lhings.java.exception.LhingsException;
import com.lhings.java.model.Device;

public class Oven extends LhingsDevice {

    private static final int degreesPerMinute = 50;
    private static final int ambientTemperature = 20;

    private int initialTemperature, setPointTemperature;
    private long bakeTime, stopTime, startTime;
    private boolean baking;
    private int i = 0;

    // defining events for your device is as simple as this!
    // you can provide an optional name
    @Event
    String finished;

    // defining status components for your device is as simple as this!
    @StatusComponent(name = "temperature")
    private int temperature;

    @StatusComponent
    private int targetTemperature;

    @StatusComponent
    private boolean hot;

    public Oven(String username, String password, String name) throws IOException, LhingsException {
        super(username, password, name);

        bakeTime = System.currentTimeMillis();
        stopTime = System.currentTimeMillis();
        setPointTemperature = 0;
        initialTemperature = ambientTemperature;
        baking = false;

    }

    public static void main(String[] args) throws InterruptedException, IOException, LhingsException {
        // provide your the username and password of your own Lhings account
        LhingsDevice device = new Oven("user@example.com", "mypassword", "My New Oven with SDK v2");
        device.setLoopFrequency(20);
        device.start();
        Thread.sleep(360000);
        device.stop();
    }

    @Override
    public void setup() {
        // In setup() you perform all the initialization tasks your device needs
        System.out.println("Oven initialization completed...");
    }

    
    // this method will be called periodically at a frequency specified by
    // the method setLoopFrequency()
    @Override
    public void loop() {
        if (isBakeFinished()) {
            sendEvent("finished");
        }

        temperature = (int) thermometer();
        targetTemperature = setPointTemperature;

        // the status component value is updated here
        hot = temperature > 30;
 
        i++;
        if (i % 100 == 0) {
            System.out.println("Temp: " + thermometer() + ", setPoint:"
                    + targetTemperature + ", baking?:" + baking);
        }
    }

    // name and description are optional, if not provided the name of the action
    // will be the name of the method, and description will be left blank.
    // You must provide the argument names, which do not need to be the same as
    // those in the Java method. 
    @Action(name = "bake", description = "action used to bake food", argumentNames = {
        "temperature", "time"})
    public void bake(int temperature, int minutes) {
        initialTemperature = (int) thermometer();
        setPointTemperature = temperature;
        startTime = System.currentTimeMillis();
        bakeTime = startTime + minutes * 60000;
        baking = true;
        System.out.println("Baking started!");
    }

    // if action has no arguments, an empty argumentNames has to be provided
    @Action(name = "show other devices", argumentNames = {})
    public void show() throws LhingsException, IOException {
        List<Device> devices = this.getDevices();
        System.out.println("Devices in the same account as me:");
        String uuidPlugLhings = null;
        for (Device device : devices) {
            System.out.println(device.getName() + " - " + device.getUuidString());
            if (device.getName().equalsIgnoreCase("pluglhings")) {
                uuidPlugLhings = device.getUuidString();
            }
        }

        if (uuidPlugLhings != null) {
            Map<String, Object> status = getStatus(uuidPlugLhings);
            Object batteryStatus = status.get("BATTERY STATUS");
            if (batteryStatus == null) {
                System.out.println("Status component BATTERY STATUS disabled for device PlugLhings. Enable it and try again.");
            } else {
                System.out.println("Battery level is: " + status.get("BATTERY STATUS").toString());
            }
        } else {
            // you should install PlugLhings mobile app!! 
            System.out.println("No PlugLhings device found");
        }

    }

    public boolean isBakeFinished() {
        if (baking && System.currentTimeMillis() > bakeTime) {
            initialTemperature = (int) thermometer();
            baking = false;
            stopTime = System.currentTimeMillis();
            System.out.println("Bake finished!");
            return true;
        } else {
            return false;
        }
    }

    private long thermometer() {
        long t;
        if (baking) {
            t = initialTemperature
                    + (long) ((System.currentTimeMillis() - startTime) / 60000f * degreesPerMinute);
            if (t > setPointTemperature) {
                t = setPointTemperature;
            }
        } else {
            t = initialTemperature
                    - (long) ((System.currentTimeMillis() - stopTime) / 60000f * degreesPerMinute);
            if (t < ambientTemperature) {
                t = ambientTemperature;
            }
        }

        return t;
    }

}
