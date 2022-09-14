package com.dehnes.battery_capacity_tester

import com.dehnes.battery_capacity_tester.Config.cutoffCurrent
import com.dehnes.battery_capacity_tester.Config.dischargeCurrent
import com.dehnes.battery_capacity_tester.Config.dischargeVoltage
import com.dehnes.battery_capacity_tester.Config.electronicLoadAddr
import com.dehnes.battery_capacity_tester.Config.initCapacityUsedInMilliAmpereHours
import com.dehnes.battery_capacity_tester.Config.interval
import com.dehnes.battery_capacity_tester.Config.localUdpPort
import com.dehnes.battery_capacity_tester.Config.timeLimit
import mu.KotlinLogging
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

object Config {
    val electronicLoadAddr = "192.168.1.17:18190"
    val localUdpPort = 18190

    // setup limits:
    val dischargeVoltage = 2.75
    val dischargeCurrent = 2.0
    val timeLimit = Duration.ofSeconds(3600)

    val initCapacityUsedInMilliAmpereHours = 0.0

    val cutoffCurrent = 0.05 // 50mA
    val interval = Duration.ofSeconds(1)
}

val executorService = Executors.newCachedThreadPool {
    Thread(it).apply { isDaemon = true }
}
val logger = KotlinLogging.logger { }

fun main() {
    // setup
    val scpiChannel = UdpSCPIChannel(
        InetAddress.getByName(electronicLoadAddr.split(":")[0]),
        electronicLoadAddr.split(":")[1].toInt(),
        localUdpPort,
        executorService
    ).apply { start() }
    val startedAt = Instant.now()

    scpiChannel.setConstantCurrent(dischargeCurrent)
    scpiChannel.enableInput()

    // control loop
    try {
        var constantVoltage = false
        var lastMeasured = Instant.now().toEpochMilli()
        var sumMilliAmpereHours = initCapacityUsedInMilliAmpereHours
        while (true) {
            Thread.sleep(interval.toMillis())

            val current = scpiChannel.getMeasuredCurrent()
            val voltage = scpiChannel.getMeasuredVoltage()
            val now = Instant.now().toEpochMilli()
            val duration = Duration.ofMillis(now - lastMeasured)

            if (voltage < dischargeVoltage && !constantVoltage) {
                scpiChannel.setConstantVoltage(dischargeVoltage)
                constantVoltage = true
            }
            if (startedAt.plusSeconds(timeLimit.toSeconds()).toEpochMilli() < now) {
                logger.info { "Time limit reached" }
                break
            }
            if (current < cutoffCurrent) {
                logger.info { "cutoffCurrent reached" }
                break
            }

            // calc drawn capacity
            val durationInSeconds = duration.toMillis().toDouble() / 1000
            val durationInHours = durationInSeconds / 3600
            val milliAmpereHours = (current * 1000) * durationInHours
            sumMilliAmpereHours += milliAmpereHours
            logger.info { "duration=${duration.toMillis()} voltage=$voltage current=$current sumMilliAmpereHours=${sumMilliAmpereHours.toDecimalString()} milliAmpereHours=${milliAmpereHours.toDecimalString()}" }

            lastMeasured = now
        }
    } finally {
        scpiChannel.disableInput()
    }
}

fun SCPIChannel.enableInput() {
    this.rpc(":INPut ON", 0)
    check(this.rpc(":INPut?").single() == "ON")
}

fun SCPIChannel.setConstantVoltage(voltage: Double) {
    this.rpc(":VOLT ${voltage.toDecimalString(3)}V", 0)
    check(this.rpc(":VOLT?").single() == voltage.toDecimalString(3) + "V")
}


fun SCPIChannel.setConstantCurrent(current: Double) {
    this.rpc(":CURR ${current.toDecimalString(4)}A", 0)
    check(this.rpc(":CURR?").single() == current.toDecimalString(4) + "A")
}

fun SCPIChannel.getMeasuredCurrent() = this.rpc(":MEASure:CURRent?").single().replace("A", "").toDouble()
fun SCPIChannel.getMeasuredVoltage() = this.rpc(":MEASure:VOLTage?").single().replace("V", "").toDouble()
fun SCPIChannel.disableInput() {
    this.rpc(":INPut OFF", 0)
    check(this.rpc(":INPut?").single() == "OFF")
}

fun Double.toDecimalString(decimals: Int = 3) = String.format("%.${decimals}f", this)