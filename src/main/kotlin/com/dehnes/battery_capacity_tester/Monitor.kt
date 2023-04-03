package com.dehnes.battery_capacity_tester

import java.net.InetAddress
import java.time.Instant

fun main() {
    val electronicLoadAddr = "192.168.1.17"
    val electronicLoadPort = 18190
    val localUdpPort = 18190


    val scpiChannel = UdpSCPIChannel(
        InetAddress.getByName(electronicLoadAddr),
        electronicLoadPort,
        localUdpPort,
        executorService
    ).apply { start() }

    while (true) {
        Thread.sleep(5000)

        val measuredVoltage = scpiChannel.getMeasuredVoltage()
        val measuredCurrent = scpiChannel.getMeasuredCurrent()

        println(measuredVoltage.toDecimalString() + "V " + measuredCurrent.toDecimalString() + "A")
    }
}