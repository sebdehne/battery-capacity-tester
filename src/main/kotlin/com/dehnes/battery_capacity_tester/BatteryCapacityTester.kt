package com.dehnes.battery_capacity_tester

import mu.KotlinLogging
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

val initCapacityUsedInMilliAmpereHours = 0.0

val interval = Duration.ofSeconds(1)

val executorService = Executors.newCachedThreadPool {
    Thread(it).apply { isDaemon = true }
}
val logger = KotlinLogging.logger { }

enum class Mode {
    CC,
    CV
}

@Volatile
var stop = false

val shutdownSync = CountDownLatch(1)

fun main(vararg args: String) {

    check(args.size > 5) { "Invalid argument. Usage: <host>:<port> <localUdpPort> <dischargeVoltage> <dischargeCurrent> <timeLimitSeconds> <cutoffCurrent>" }
    val electronicLoadAddr = args[0].split(":")[0]
    val electronicLoadPort = args[0].split(":")[1].toInt()
    val localUdpPort = args[1].toInt()
    val dischargeVoltage = args[2].toDouble()
    val dischargeCurrent = args[3].toDouble()
    val timeLimit = Duration.ofSeconds(args[4].toLong())
    val cutoffCurrent = args[5].toDouble()

    // gracefull shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        stop = true
        shutdownSync.await()
    })

    // setup
    val scpiChannel = UdpSCPIChannel(
        InetAddress.getByName(electronicLoadAddr),
        electronicLoadPort,
        localUdpPort,
        executorService
    ).apply { start() }
    val startedAt = Instant.now()

    // start in CC mode
    scpiChannel.setConstantCurrent(dischargeCurrent)
    var mode = Mode.CC

    // Let's go
    scpiChannel.enableInput()

    // control loop
    try {
        var lastMeasured = Instant.now().toEpochMilli()
        var sumMilliAmpereHours = initCapacityUsedInMilliAmpereHours
        while (true) {
            Thread.sleep(interval.toMillis())

            val current = scpiChannel.getMeasuredCurrent()
            val voltage = scpiChannel.getMeasuredVoltage()
            val now = Instant.now().toEpochMilli()
            val duration = Duration.ofMillis(now - lastMeasured)

            if (!scpiChannel.isInputEnabled()) {
                logger.info { "Input disabled, exit" }
                break
            }
            if (stop) {
                logger.info { "Exit" }
                break
            }

            if (voltage < dischargeVoltage && mode == Mode.CC) {
                scpiChannel.setConstantVoltage(dischargeVoltage)
                scpiChannel.enableInput()
                mode = Mode.CV
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
            logger.info {
                " mode=$mode" +
                        " duration=${duration.toMillis()}" +
                        " voltage=${voltage.toDecimalString()}" +
                        " current=${current.toDecimalString()}" +
                        " sumMilliAmpereHours=${sumMilliAmpereHours.toDecimalString()}" +
                        " milliAmpereHours=${milliAmpereHours.toDecimalString()}"
            }

            lastMeasured = now
        }
    } finally {
        scpiChannel.disableInput()
        scpiChannel.stop()
        shutdownSync.countDown()
    }

}

fun SCPIChannel.enableInput() {
    this.rpc(":INPut ON", 0)
    check(this.rpc(":INPut?").single() == "ON")
}

fun SCPIChannel.setConstantVoltage(voltage: Double) {
    this.rpc(":VOLT ${voltage.toDecimalString(4)}V", 0)
    check(this.rpc(":VOLT?").single() == voltage.toDecimalString(4) + "V")
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

fun SCPIChannel.isInputEnabled() = this.rpc(":INPut?").single() == "ON"

fun Double.toDecimalString(decimals: Int = 3) = String.format("%.${decimals}f", this)